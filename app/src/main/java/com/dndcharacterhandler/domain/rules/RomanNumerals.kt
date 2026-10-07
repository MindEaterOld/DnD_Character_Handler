package com.dndcharacterhandler.domain.rules

/** [number] in Roman numerals, as the overview's portrait writes the level ("XIV"); 0 and below have none: "". */
fun romanNumeral(number: Int): String {
    if (number <= 0) return ""
    val numerals = listOf(
        1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC",
        50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I"
    )
    var left = number
    return buildString {
        numerals.forEach { (value, letters) ->
            while (left >= value) {
                append(letters)
                left -= value
            }
        }
    }
}
