package com.tetrisprime.kotlin

import kotlin.random.Random

/**
 * 7-bag randomizer — ensures fair piece distribution.
 * Each batch of 7 contains all 7 piece types in random order.
 *
 * Supports seeded RNG for deterministic daily challenges.
 */
class BagRandomizer(private val rng: Random = Random.Default) {
    private val bag = mutableListOf<PieceType>()
    private var index = 0

    init {
        refill()
    }

    private fun refill() {
        bag.clear()
        bag.addAll(PieceType.values().toList())
        bag.shuffle(rng)
        index = 0
    }

    fun next(): PieceType {
        if (index >= bag.size) {
            refill()
        }
        return bag[index++]
    }

    /** Preview the next N pieces without consuming them. */
    fun preview(n: Int): List<PieceType> {
        val result = mutableListOf<PieceType>()
        var i = index
        var tempBag = bag.toList()
        while (result.size < n) {
            if (i >= tempBag.size) {
                // Generate a new bag for preview
                tempBag = PieceType.values().toMutableList().also { it.shuffle(rng) }
                i = 0
            }
            result.add(tempBag[i++])
        }
        return result
    }
}

/**
 * FNV-1a hash function for deterministic daily challenge seeding.
 * Matches the JavaScript implementation: seed=0x811c9dc5, prime=0x1000193
 */
object Fnv1aHash {
    private const val FNV_OFFSET_BASIS: Long = 0x811c9dc5L
    private const val FNV_PRIME: Long = 0x1000193L
    private const val MOD: Long = 0x100000000L // 2^32

    /** Compute FNV-1a hash of a string. Returns a 32-bit unsigned hash as Long. */
    fun hash(str: String): Long {
        var hash = FNV_OFFSET_BASIS
        for (ch in str) {
            hash = (hash xor ch.code.toLong()) % MOD
            hash = (hash * FNV_PRIME) % MOD
        }
        return hash
    }

    /**
     * Generate the daily challenge goal from a date string.
     * @param dateStr Date in YYYY-MM-DD format
     * @return Goal score in range 9000..15250
     */
    fun dailyGoal(dateStr: String): Int {
        val seed = "goal/$dateStr"
        val h = hash(seed)
        return 9000 + ((h % 25).toInt()) * 250
    }
}

/**
 * Seeded random number generator for deterministic daily challenges.
 * Uses a simple Linear Congruential Generator (LCG) seeded by FNV-1a hash.
 */
class SeededRandom(seed: Long) {
    private var state = seed

    /** Next pseudo-random Int in [0, until). */
    fun nextInt(until: Int): Int {
        state = (state * 6364136223846793005L + 1442695040888963407L) and 0x7FFFFFFFFFFFFFFFL
        return ((state shr 33) % until).toInt()
    }

    /** Next pseudo-random Float in [0, 1). */
    fun nextFloat(): Float {
        state = (state * 6364136223846793005L + 1442695040888963407L) and 0x7FFFFFFFFFFFFFFFL
        return (state shr 40).toFloat() / (1 shl 24).toFloat()
    }

    /** Shuffle a list in-place using Fisher-Yates. */
    fun <T> shuffle(list: MutableList<T>) {
        for (i in list.size - 1 downTo 1) {
            val j = nextInt(i + 1)
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
    }
}

/**
 * Seeded 7-bag randomizer for daily challenges.
 * Produces the same piece sequence for the same seed.
 */
class SeededBagRandomizer(seed: Long) {
    private val rng = SeededRandom(seed)
    private val bag = mutableListOf<PieceType>()
    private var index = 0

    init {
        refill()
    }

    private fun refill() {
        bag.clear()
        bag.addAll(PieceType.values().toList())
        rng.shuffle(bag)
        index = 0
    }

    fun next(): PieceType {
        if (index >= bag.size) {
            refill()
        }
        return bag[index++]
    }

    fun preview(n: Int): List<PieceType> {
        val result = mutableListOf<PieceType>()
        var i = index
        var tempBag = bag.toList()
        while (result.size < n) {
            if (i >= tempBag.size) {
                tempBag = PieceType.values().toMutableList().also { rng.shuffle(it) }
                i = 0
            }
            result.add(tempBag[i++])
        }
        return result
    }
}
