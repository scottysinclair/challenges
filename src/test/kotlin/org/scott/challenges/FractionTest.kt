package org.scott.challenges

import org.junit.jupiter.api.Test
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.math.MathContext
import java.util.regex.Pattern

/*
 * The challenge: https://edabit.com/challenge/YLdgd8dav2joTpXbn
 *
 */

/**
 * The number specification
 */
data class NumberSpec(val specText: String) {
   val beforePoint: String
   val afterPoint: String
   val repeatingPart: String
   init {
      val matcher = Pattern.compile("(\\d+)\\.(\\d*)\\((\\d+)\\)").matcher(specText)
      matcher.find()
      beforePoint = matcher.group(1)
      afterPoint = matcher.group(2)
      repeatingPart = matcher.group(3)
   }
   /*
    * The required precision to test to, so that we are confident the answer is correct
    */
   fun getRequiredPrecision() : Int {
      /*
       * we multiply the repeating part by 3 so that we test to a high precision.
       * the precision has to be at least 10 decimal places
       */
      val desiredPrecision = (beforePoint.length + afterPoint.length + (repeatingPart.length * 3)).takeUnless { it < 10 } ?: 10

      /*
       * the precision of the number spec before the repeating part is reached.
       */
      val precisionBeforeRepeat = "$beforePoint$afterPoint".length
      /*
       * how many times the repeating part needs to repeat to reach the desired precision
       */
      var numberOfRepeats = (desiredPrecision - precisionBeforeRepeat) / repeatingPart.length
      if ((desiredPrecision - precisionBeforeRepeat) % repeatingPart.length != 0) {
         numberOfRepeats++
      }
      //generate the characters we want without the '.' that it our reequired precision
      return "$beforePoint$afterPoint${(1..numberOfRepeats).map { repeatingPart }.joinToString("")}".length
   }

   /*
    * the required scale is calculated from the required precision minus the number of digits before the point
    */
   fun getRequiredScale() : Int {
      return getRequiredPrecision() - beforePoint.length
   }

   /*
    * converts the number spec to a BigDecimal
    */
   fun toBigDecimal() : BigDecimal {
      /*
       * the required precision of the BigDecimal we need to generate from the spec for testing
       */
      val precisionBeforeRepeat = "$beforePoint$afterPoint".length
      /*
       * how many times the repeating part needs to repeat to reach the desired precision,
       * this always divides exactly due to the implementation of 'getRequiredPrecision()'
       */
      val numberOfRepeats = (getRequiredPrecision() - precisionBeforeRepeat) / repeatingPart.length
      /*
       * generate the BigDecimal with the desired precision and number of repeats
       */
      return "$beforePoint.$afterPoint${(1..numberOfRepeats).map { repeatingPart }.joinToString("")}".let {
         it.toBigDecimal(MathContext(getRequiredPrecision()))
      }
   }
   override fun toString() : String {
      return "NumberSpec( $beforePoint.$afterPoint($repeatingPart) )"
   }
}

/*
 * A fraction with a top and a bottom
 */
data class Fraction(val top: Long, val bottom: Long) {
   /*
    * converts the Fraction into a BigDecimal with the required precision and scale
    */
   fun toBigDecimal(requiredPrecision: Int, requiredScale: Int) : BigDecimal {
      return top.toBigDecimal(MathContext(requiredPrecision)).setScale(requiredScale) / bottom.toBigDecimal().setScale(requiredScale)
   }
   override fun toString() : String {
      return "$top / $bottom"
   }
}

/**
 * A TestNode which has a NumberSpec and a Fraction which is tested against the NumberSpec
 */
class FractionTestNode(val parent: FractionTestNode?, val numberSpec: NumberSpec, val fraction: Fraction) {
   /**
    * @return true if the fraction is an accurate representation of the NumberSpec
    */
   fun isAccurateRepresentation() : Boolean {
      return numberOfDigitsAccurate() == numberSpec.getRequiredPrecision()
   }

   /**
    * Compares the NumberSpec BigDecmial to the Fraction BigDecimal digit by digit
    * and returns the number of digits accurate.
    */
   fun numberOfDigitsAccurate() : Int {
      val charsSpec = numberSpec.toBigDecimal().toPlainString().toList()
      val charsNode = getFractionAsDecimal().toPlainString().toList()
      var count = 0
      var i = 0
      while(i <= numberSpec.getRequiredPrecision() && charsSpec[i] == charsNode[i]) {
         if (charsSpec[i] != '.') count++
         i++
      }
      return count
   }

   /**
    * Gets the digit at a given position in the number
    */
   private fun getDigitAtPosition(number: String, pos: Int) : Int {
      var digitCount = 0
      var i = 0
      while(i < number.length && digitCount < pos) {
         if (number[i].isDigit()) {
            digitCount++
         }
         i++
      }
      if (digitCount == pos) {
         return number[i-1].digitToInt()
      }
      throw IllegalStateException("Could not find digit at pos $pos")
   }

   /**
    * Compares the difference between the NumberSpec and the Fraction at a specific position in the number
    */
   fun differenceAt(pos: Int) : Int {
      val specDigit = getDigitAtPosition(numberSpec.toBigDecimal().toPlainString(), pos)
      val nodeDigit = getDigitAtPosition(getFractionAsDecimal().toPlainString(), pos)
      return nodeDigit - specDigit
   }

   /**
    * gets the Fraction as a BigDecimal which has the required precision + 1. The '+1' ensures
    * that no rounding will occur with the required precision part of the BigDecimal
    */
   private fun getFractionAsDecimal() : BigDecimal {
      //plus 1 because we need the round up to occur after the required precision so we match (0.66666666)7  and not (0.66666667)
      return fraction.toBigDecimal(numberSpec.getRequiredPrecision() + 1, numberSpec.getRequiredScale() + 1)
   }

   /**
    * @return a new FractionTestNode with the Fraction bottom incremented by the given amount
    */
   fun incrementBottomBy(amount : Int) : FractionTestNode {
      return FractionTestNode(this, numberSpec, fraction.copy(bottom = fraction.bottom + amount))
   }

   /**
    * @return a new FractionTestNode with the Fraction top incremented by the given amount
    */
   fun incrementTopBy(amount : Int) : FractionTestNode {
      return FractionTestNode(this, numberSpec, fraction.copy(top = fraction.top + amount))
   }

   /**
    * the number of FractionTestNodes we have created so far to solve the problem
    */
   fun depth() : Int {
      return if (parent == null) 0
      else parent.depth() + 1
   }

   override fun toString() : String {
      return if (isAccurateRepresentation()) {
         "$numberSpec  => $fraction  Fully accurate to ${numberOfDigitsAccurate()} decimal places ✅"
      }
      else "$numberSpec  => $fraction  Accurate only to ${numberOfDigitsAccurate()} decimal places ❌"
   }
}

/**
 * The actual function...
 */
fun fraction(text : String) : FractionTestNode {
   /*
    * create the NumberSpec for the given input
    */
   val numberSpec = NumberSpec(text)
   /*
    * create a FractionTestNode which we use as our starting point
    */
   var fractionTestNode = FractionTestNode(null, numberSpec, Fraction(numberSpec.beforePoint.toLong(), 1))

   /*
    * keep on testing until we have tested 100000 times or we reach the required accuracy
    */
   while(fractionTestNode.depth() < 100000 && fractionTestNode.isAccurateRepresentation().not()) {
      /*
       * how many digits are accurate
       */
      val accuracy = fractionTestNode.numberOfDigitsAccurate()
      /*
       * compare the first digit which differs from the spec
       */
      val cmp = fractionTestNode.differenceAt(accuracy + 1)
      when {
         /*
          * the Fraction number is too high, so increase the bottom of the fraction to shrink the number
          */
         cmp > 0 -> fractionTestNode = fractionTestNode.incrementBottomBy(1)
         /*
          * the Fraction number is too low, so increase the top of the fraction to shrink the number
          */
         cmp < 0 -> fractionTestNode = fractionTestNode.incrementTopBy(1)
      }
   }
   return fractionTestNode
}


class FractionTest {

   @Test
   fun test() {
      "2.(2)".let {
         val result = fraction(it)
         println("$it   =>    $result      search depth: ${result.depth()}")
      }

      "1.(1)".let {
         val result = fraction(it)
         println("$it   =>    $result      search depth: ${result.depth()}")
      }

      "0.(6)".let {
         val result = fraction(it)
         println("$it   =>    $result      search depth: ${result.depth()}")
      }

      "3.(142857)".let {
         val result = fraction(it)
         println("$it   =>    $result      search depth: ${result.depth()}")
      }

      "0.19(2367)".let {
         val result = fraction(it)
         println("$it   =>    $result      search depth: ${result.depth()}")
      }

      "0.1097(3)".let {
         val result = fraction(it)
         println("$it   =>    $result      search depth: ${result.depth()}")
      }
   }
}