package com.mangauniversal.app.domain.util

import java.util.Comparator

class NaturalOrderComparator : Comparator<String> {

    override fun compare(s1: String?, s2: String?): Int {
        if (s1 == null || s2 == null) return 0

        var i1 = 0
        var i2 = 0
        val len1 = s1.length
        val len2 = s2.length

        while (i1 < len1 && i2 < len2) {
            val c1 = s1[i1]
            val c2 = s2[i2]

            if (c1.isDigit() && c2.isDigit()) {
                val num1 = getChunk(s1, len1, i1)
                val num2 = getChunk(s2, len2, i2)

                i1 += num1.length
                i2 += num2.length

                val diff = num1.length - num2.length
                if (diff != 0) {
                    val trimmed1 = num1.trimStart('0')
                    val trimmed2 = num2.trimStart('0')
                    if (trimmed1.length != trimmed2.length) {
                        return trimmed1.length - trimmed2.length
                    }
                    val cmp = trimmed1.compareTo(trimmed2)
                    if (cmp != 0) return cmp
                } else {
                    val cmp = num1.compareTo(num2)
                    if (cmp != 0) return cmp
                }
            } else {
                val cmp = c1.lowercaseChar().compareTo(c2.lowercaseChar())
                if (cmp != 0) return cmp
                i1++
                i2++
            }
        }
        return len1 - len2
    }

    private fun getChunk(s: String, length: Int, start: Int): String {
        val chunk = StringBuilder()
        var ptr = start
        while (ptr < length && s[ptr].isDigit()) {
            chunk.append(s[ptr])
            ptr++
        }
        return chunk.toString()
    }
}
