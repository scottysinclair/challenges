package org.scott.challenges

import org.junit.jupiter.api.Test
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.math.MathContext
import java.util.regex.Pattern

//https://edabit.com/challenge/YLdgd8dav2joTpXbn
data class NumberSpec(val specText: String) {
   val beforePoint: String
   val afterPoint: String
   val repeating: String
   init {
      val matcher = Pattern.compile("(\\d+)\\.(\\d*)\\((\\d+)\\)").matcher(specText)
      matcher.find()
      beforePoint = matcher.group(1)
      afterPoint = matcher.group(2)
      repeating = matcher.group(3)
   }
   /*
    * The required precision to test so that we are confident the answer is correct
    */
   fun getRequiredPrecision() : Int {
      val desiredPrecision = (beforePoint.length + afterPoint.length + (repeating.length * 3)).takeUnless { it < 10 } ?: 10

      /*
       * the required precision of the BigDecimal we need to generate from the spec for testing
       */
      val precisionBeforeRepeat = "$beforePoint$afterPoint".length
      /*
       * how many times the repeating part needs to repeat to reach the desired precision
       */
      var numberOfRepeats = (desiredPrecision - precisionBeforeRepeat) / repeating.length
      if ((desiredPrecision - precisionBeforeRepeat) % repeating.length != 0) {
         numberOfRepeats++
      }
      //generate the characters we want without the '.' that it our reequired precision
      return "$beforePoint$afterPoint${(1..numberOfRepeats).map { repeating }.joinToString("")}".length
   }

   fun getRequiredScale() : Int {
      return getRequiredPrecision() - beforePoint.length
   }
   fun toBigDecimal() : BigDecimal {
      /*
       * the required precision of the BigDecimal we need to generate from the spec for testing
       */
      val precisionBeforeRepeat = "$beforePoint$afterPoint".length
      /*
       * how many times the repeating part needs to repeat to reach the desired precision
       */
      var numberOfRepeats = (getRequiredPrecision() - precisionBeforeRepeat) / repeating.length
      /*
       * generate the BigDecimal with the desired precision and number of repeats
       */
      return "$beforePoint.$afterPoint${(1..numberOfRepeats).map { repeating }.joinToString("")}".let {
         it.toBigDecimal(MathContext(getRequiredPrecision()))
      }
   }
   override fun toString() : String {
      return "NumberSpec( $beforePoint.$afterPoint($repeating) )"
   }
}
data class Fraction(val top: Long, val bottom: Long) {
   fun toBigDecimal(requiredPrecision: Int, requiredScale: Int) : BigDecimal {
      return top.toBigDecimal(MathContext(requiredPrecision)).setScale(requiredScale) / bottom.toBigDecimal().setScale(requiredScale)
   }
   override fun toString() : String {
      return "$top / $bottom"
   }
}


class FractionTestNode(val parent: FractionTestNode?, val numberSpec: NumberSpec, val fraction: Fraction) {
   fun isCompletelyAccurate() : Boolean {
      return numberOfDigitsAccurate() == numberSpec.getRequiredPrecision()
   }
   fun numberOfDigitsAccurate() : Int {
      var charsSpec = numberSpec.toBigDecimal().toPlainString().toList()
      var charsNode = getFractionAsDecimal().toPlainString().toList()
      var count = 0
      var i = 0
      while(i <= numberSpec.getRequiredPrecision() && charsSpec[i] == charsNode[i]) {
         if (charsSpec[i] != '.') count++
         i++
      }
      return count
   }
   fun getDigitAtPosition(number: String, pos: Int) : Int {
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
   fun differenceAt(pos: Int) : Int {
      val specDigit = getDigitAtPosition(numberSpec.toBigDecimal().toPlainString(), pos)
      val nodeDigit = getDigitAtPosition(getFractionAsDecimal().toPlainString(), pos)
      return nodeDigit - specDigit
   }
   fun getFractionAsDecimal() : BigDecimal {
      //plus 1 because we need the round up to occur after the required precision so we match (0.66666666)7  and not (0.66666667)
      return fraction.toBigDecimal(numberSpec.getRequiredPrecision() + 1, numberSpec.getRequiredScale() + 1)
   }

   fun getDifferenceFromSpec() : BigDecimal  {
      return getFractionAsDecimal() - numberSpec.toBigDecimal()
   }
   fun multiplyTopAndBottomBy(amount : Int) : FractionTestNode {
      return FractionTestNode(this, numberSpec, fraction.copy(top = fraction.top * amount, bottom = fraction.bottom * 2))
   }
   fun incrementBottomBy(amount : Int) : FractionTestNode {
      return FractionTestNode(this, numberSpec, fraction.copy(bottom = fraction.bottom + amount))
   }
   fun incrementTopBy(amount : Int) : FractionTestNode {
      return FractionTestNode(this, numberSpec, fraction.copy(top = fraction.top + amount))
   }
   fun depth() : Int {
      return if (parent == null) 0
      else parent.depth() + 1
   }
   override fun toString() : String {
      return if (isCompletelyAccurate()) {
         "$numberSpec  => $fraction  Fully accurate to ${numberOfDigitsAccurate()} decimal places ✅"
      }
      else "$numberSpec  => $fraction  Accurate only to ${numberOfDigitsAccurate()} decimal places ❌"
   }
}

fun fraction(text : String) : FractionTestNode {
   val numberSpec = NumberSpec(text)
   var fractionTestNode = FractionTestNode(null, numberSpec, Fraction(numberSpec.beforePoint.toLong(), 1))
   while(fractionTestNode.depth() < 1000 && fractionTestNode.isCompletelyAccurate().not()) {
      //println(fractionTestNode)
      val accuracy = fractionTestNode.numberOfDigitsAccurate()
      if (accuracy < fractionTestNode.numberSpec.getRequiredPrecision()) {
         val cmp = fractionTestNode.differenceAt(accuracy + 1)
         when {
            cmp > 0 -> fractionTestNode = fractionTestNode.incrementBottomBy(1)
            cmp < 0 -> fractionTestNode = fractionTestNode.incrementTopBy(1)
         }
      }
   }
   return fractionTestNode
}


fun smallestDifferenceFromSpec(a: FractionTestNode, b: FractionTestNode) : FractionTestNode {
   return if (a.getDifferenceFromSpec() < b.getDifferenceFromSpec()) a else b
}


class FractionTest {

   @Test
   fun test() {
      "2.(2)".let { println("$it   =>    ${fraction(it)}") }

      "1.(1)".let { println("$it   =>    ${fraction(it)}") }

      "0.(6)".let { println("$it   =>    ${fraction(it)}") }

      "3.(142857)".let { println("$it   =>    ${fraction(it)}") }

      "3.(142857)".let { println("$it   =>    ${fraction(it)}") }

      "0.19(2367)".let { println("$it   =>    ${fraction(it)}") }

      "0.1097(3)".let { println("$it   =>    ${fraction(it)}") }
   }
}

/*
/*
package org.scott.challenges

import java.math.RoundingMode
import java.util.regex.Pattern

// https://edabit.com/challenge/YLdgd8dav2joTpXbn

fun fraction(text: String) : String {
   val matcher = Pattern.compile("(\\d+)\\.(\\d*)\\((\\d+)\\)").matcher(text)
   matcher.find()
   val start = matcher.group(1)
   val after = matcher.group(2)
   val repeat = matcher.group(3)
//   println("$text     $start  $after  $repeat")
   return fraction(start, after, repeat, 1, 1, 1).let { (top, bottom) -> "$top/$bottom" }
}

fun fraction(start: String, after: String, repeat:  String, top: Int, bottom: Int, accuracy: Int) : Pair<Int,Int> {
   //println("top $top  bottom $bottom  acc $accuracy")
   val result = top.toBigDecimal().setScale(20, RoundingMode.HALF_EVEN) / bottom.toBigDecimal()
   //println("${result.toPlainString()}       $start.$after($repeat)")
   return if (accuracy <= start.length) {
      val cmp = compare(result.toPlainString().substring(0, accuracy), start, after, repeat)
      when {
         cmp > 0 -> fraction(start, after, repeat, top, bottom + 1, accuracy)
         cmp < 0 -> fraction(start, after, repeat, top + 1, bottom, accuracy)
         else -> fraction(start, after, repeat, top, bottom, accuracy + 1)

      }
   }
   else if (accuracy <= (start.length + after.length + 1)) {
      val cmp = compare(result.toPlainString().substring(0, accuracy), start, after, repeat)
      when {
         cmp > 0 -> fraction(start, after, repeat, top, bottom + 1, accuracy)
         cmp < 0 -> fraction(start, after, repeat, top + 1, bottom, accuracy)
         else -> fraction(start, after, repeat, top, bottom, accuracy + 1)

      }
   }
   else if (accuracy <= (start.length + after.length + 1 + (repeat.length * 4) ) ) {
      val cmp = compare(result.toPlainString().substring(0, accuracy), start, after, repeat)
      when {
         cmp > 0 -> fraction(start, after, repeat, top, bottom + 1, accuracy)
         cmp < 0 -> fraction(start, after, repeat, top + 1, bottom, accuracy)
         else -> fraction(start, after, repeat, top, bottom, accuracy + 1)
      }
   }
   else if (accuracy > (start.length + after.length + 1 + (repeat.length * 4) ) ) {
      top to bottom
   }
   else -1 to -1
}

fun compare(number: String, start: String, after: String, repeat: String) : Int {
   //println("compare $number  $start.$after($repeat)")
   var i = 0
   var cmp = 0
   while(i < number.length && cmp == 0) {
      if (number[i] != '.') {
         if (i < start.length) {
            cmp = number[i] - start[i]
            //println("cmp: $i  ${number[i]}  ${start[i]}  $cmp")
         } else if (i > start.length + 1 && i < (start.length + 1 + after.length)) {
            cmp = number[i] - after[i - start.length + 1]
            //println("cmp: $i  ${number[i]}  ${after[i - start.length]}  $cmp")
         } else {
            val pos = i - (start.length + 1 + after.length)
            val adjustedPos = pos % repeat.length
            cmp = number[i] - repeat[adjustedPos]
            //println("cmp: $i  ${number[i]}  ${repeat[adjustedPos]}  $cmp")
         }
      }
      i++
   }
   return cmp
}

 */