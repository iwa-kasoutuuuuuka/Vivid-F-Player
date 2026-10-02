package com.example.videoplayer.util

import java.util.Comparator

/**
 * 1, 2, 10 のように数字を考慮した自然順ソートを行うためのコンパレータです。
 * オブジェクトアロケーションをゼロにし、超高速に比較します。
 */
object NaturalOrderComparator : Comparator<String> {
    override fun compare(s1: String, s2: String): Int {
        var i = 0
        var j = 0
        val len1 = s1.length
        val len2 = s2.length

        while (i < len1 && j < len2) {
            val c1 = s1[i]
            val c2 = s2[j]

            if (c1.isDigit() && c2.isDigit()) {
                // 先頭ゼロのカウント
                var zeroCount1 = 0
                while (i < len1 && s1[i] == '0') {
                    zeroCount1++
                    i++
                }
                var zeroCount2 = 0
                while (j < len2 && s2[j] == '0') {
                    zeroCount2++
                    j++
                }

                // 有効数字の桁数と先頭文字の走査
                val start1 = i
                while (i < len1 && s1[i].isDigit()) i++
                val numLen1 = i - start1

                val start2 = j
                while (j < len2 && s2[j].isDigit()) j++
                val numLen2 = j - start2

                // 桁数が違えば、桁数が多い方が大きい
                if (numLen1 != numLen2) {
                    return numLen1 - numLen2
                }

                // 桁数が同じなら、上位桁から順に文字比較（数値の大小と完全に一致）
                for (k in 0 until numLen1) {
                    val d1 = s1[start1 + k]
                    val d2 = s2[start2 + k]
                    if (d1 != d2) {
                        return d1 - d2
                    }
                }

                // 数値として完全に同じ場合、先頭ゼロの数で比較（例: 01 と 1）
                if (zeroCount1 != zeroCount2) {
                    return zeroCount1 - zeroCount2
                }
            } else {
                if (c1 != c2) {
                    return c1 - c2
                }
                i++
                j++
            }
        }
        return len1 - len2
    }
}
