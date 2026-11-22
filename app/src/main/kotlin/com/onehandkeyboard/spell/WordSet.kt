package com.onehandkeyboard.spell

import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Whether a word exists, for a word list far larger than the dictionary (every form of every
 * Spanish word) in under 1 MB: a Bloom filter over folded words, built by
 * tools/build_word_set.py. A "no" is always right; a "yes" is wrong for ~1 % of non-words.
 *
 * It has no frequencies nor accents, so it never makes a word "correct" by itself: it only
 * stops the keyboard from forcing a correction on a rare word that exists ([Suggester]).
 */
class WordSet private constructor(
    private val bits: ByteArray,
    private val bitCount: Long,
    private val hashes: Int,
) {
    operator fun contains(word: String): Boolean {
        val data = WordDictionary.fold(word).toByteArray(Charsets.UTF_8)
        if (data.isEmpty()) return false
        val h1 = fnv1a(data, OFFSET_1)
        val h2 = fnv1a(data, OFFSET_2) or 1L
        return (0 until hashes).all { isSet(java.lang.Long.remainderUnsigned(h1 + it * h2, bitCount)) }
    }

    private fun isSet(bit: Long) =
        bits[(bit ushr BYTE_SHIFT).toInt()].toInt() and (1 shl (bit and BIT_IN_BYTE).toInt()) != 0

    companion object {
        private const val MAGIC = 0x57534554 // "WSET"
        private val OFFSET_1 = 0xCBF29CE484222325uL.toLong()
        private val OFFSET_2 = 0x84222325CBF29CE4uL.toLong()
        private const val FNV_PRIME = 0x100000001B3L
        private const val BYTE_SHIFT = 3
        private const val BIT_IN_BYTE = 7L
        private const val BITS_PER_BYTE = 8
        private const val UNSIGNED_BYTE = 0xFFL

        private fun fnv1a(
            data: ByteArray,
            offset: Long,
        ): Long {
            var h = offset
            for (byte in data) h = (h xor (byte.toLong() and UNSIGNED_BYTE)) * FNV_PRIME
            return h
        }

        fun read(input: InputStream): WordSet {
            val data = DataInputStream(input.buffered())
            if (data.readInt() != MAGIC) throw IOException("Not a word set")
            val bitCount = data.readLong()
            val hashes = data.readInt()
            if (bitCount <= 0 || hashes <= 0) throw IOException("Bad word set header")
            val bits = ByteArray(((bitCount + BITS_PER_BYTE - 1) / BITS_PER_BYTE).toInt())
            data.readFully(bits)
            return WordSet(bits, bitCount, hashes)
        }
    }
}
