package org.scott.challenges

import org.junit.jupiter.api.Test

//https://edabit.com/challenge/YzWb77MowQpixfpWh
fun ascendingConsecutive(number: String) : Boolean {
   return ascendingConsecutive(emptyList(), number, 1).size > 1
}

fun ascendingConsecutive(numbers: List<Int>, end: String, currentNumDigits: Int) : List<Int> {
   println("-> numbers $numbers  end $end  currentNumDigets $currentNumDigits")
   if (end.isEmpty()) {
      return numbers
   }
   else if (end.length < currentNumDigits) {
      return emptyList()
   }
   else {
      val baseNumber = numbers.lastOrNull()
      val nextNumber = end.substring(0, currentNumDigits).toInt()
      var result = emptyList<Int>() //empty list indicates failure
      /*
       * if the next number is consecutive to the base number then take it and proceed with the rest
       */
      if (baseNumber == null || baseNumber + 1 == nextNumber) {
         val rest = if (nextNumber.toString().length >= end.length ) "" else end.substring(nextNumber.toString().length)
         result = ascendingConsecutive(numbers + nextNumber, rest, currentNumDigits)
      }
      return if (result.isNotEmpty()) {
         /*
          * we got a successful result, return it
          */
         result
      }
      else if (currentNumDigits < end.length) {
         /*
          * nextNumber was not valid, try a larger number, if it makes sense to
          */
         if  (baseNumber == null || nextNumber < (baseNumber + 1)) {
            //this only makes sense if next number was stil smaller
            ascendingConsecutive(numbers, end, currentNumDigits + 1)
         } else emptyList()
      }
      else {
         /*
          * not a successful result and there are no more digits we add, so we have failed
          */
         emptyList()
      }
   }
}



class AscendingConsecutiveTest {

   @Test
   fun test() {
      "232425".let { println("$it    ${ascendingConsecutive(it)}") }
      "2324256".let { println("$it    ${ascendingConsecutive(it)}") }
      "444445".let { println("$it    ${ascendingConsecutive(it)}") }
      "9899100".let { println("$it    ${ascendingConsecutive(it)}") }
   }
}



/**
 *
 *
 * 0.5
 * 0.5 * 2 = 1/2
 * 1.5 = 3/2
 * .25 = 1/4
 * 2.25 = 9/4
 *
 */
