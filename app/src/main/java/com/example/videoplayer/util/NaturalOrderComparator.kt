package com.example.videoplayer.util

import java.util.Comparator

/**
 * 1, 2, 10 のように数字を考慮した自然順ソートを行うためのコンパレータです。
 */
object NaturalOrderComparator : Comparator<String> {
    override fun compare(s1: String, s2: String): Int {
        var i = 0
        var j = 0
        while (i < s1.length && j < s2.length) {
            val c1 = s1[i]
            val c2 = s2[j]
            if (c1.isDigit() && c2.isDigit()) {
                val n1 = s1.substring(i).takeWhile { it.isDigit() }
                val n2 = s2.substring(j).takeWhile { it.isDigit() }
                val num1 = n1.toBigInteger()
                val num2 = n2.toBigInteger()
                if (num1 != num2) return num1.compareTo(num2)
                i += n1.length
                j += n2.length
            } else {
                if (c1 != c2) return c1.compareTo(c2)
                i++
                j++
            }
        }
        return s1.length - s2.length
    }
}
