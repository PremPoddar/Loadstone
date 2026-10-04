package com.prempoddar.loadstone.share

import kotlin.math.roundToLong

/**
 * A location written as a sentence.
 *
 * Sharing a pin as a link only works if something can open the link, and a link to a place the
 * recipient's phone has never heard of opens a browser at best. A sentence has no such
 * requirement: it survives being read down a phone line, written on the back of a ticket, or
 * dropped into any chat app in the world, and it is typed back in by hand.
 *
 * Seven words carry the position, drawn from four 128-word vocabularies in a fixed order:
 *
 * ```
 *   adjective  noun      adverb   verb     adjective  noun      adverb
 *   "Grumpy    walruses  quietly  juggle   soggy      waffles   backwards."
 * ```
 *
 * The grammar is what makes it funny every time rather than now and then — any adjective here
 * sits on any noun here — and it is also what makes it checkable: a word in the wrong slot is
 * wrong by construction, before any arithmetic happens.
 *
 * Seven words of seven bits is 49 bits. Latitude and longitude take 23 each, which puts the
 * rounding error under about 1.2 m north–south and 2.4 m east–west at the equator — comfortably
 * finer than the GPS fix that produced the pin. The three left over are a checksum, so a sentence
 * garbled into a different but still well-formed one is rejected rather than quietly sending
 * someone to the wrong street.
 */
object WordCode {
    const val WORD_COUNT = 7

    private const val LAT_BITS = 23
    private const val LON_BITS = 23
    private const val CHECK_BITS = 3

    private const val LAT_STEPS = (1L shl LAT_BITS) - 1
    private const val LON_STEPS = (1L shl LON_BITS) - 1
    private const val CHECK_MASK = (1L shl CHECK_BITS) - 1

    /** Which vocabulary each position is drawn from. */
    private val slots: List<List<String>> by lazy {
        listOf(ADJECTIVES, NOUNS, ADVERBS, VERBS, ADJECTIVES, NOUNS, ADVERBS)
    }

    /** word -> index for each position, sharing one map between the slots that repeat. */
    private val lookups: List<Map<String, Int>> by lazy {
        val built = HashMap<List<String>, Map<String, Int>>()
        slots.map { list ->
            built.getOrPut(list) { list.withIndex().associate { (i, w) -> w to i } }
        }
    }

    sealed interface Decoded {
        data class Location(val latitude: Double, val longitude: Double) : Decoded

        /** No run of seven words in the text looked like a code at all. */
        data object NotFound : Decoded

        /** Seven plausible words, but [word] is not one this position can hold. */
        data class UnknownWord(val word: String, val position: Int) : Decoded

        /** Every word was valid but the checksum disagrees, so at least one of them is wrong. */
        data object Garbled : Decoded
    }

    /** The seven words for a position, in order. */
    fun encode(latitude: Double, longitude: Double): List<String> {
        val lat = ((latitude.coerceIn(-90.0, 90.0) + 90.0) / 180.0 * LAT_STEPS)
            .roundToLong().coerceIn(0, LAT_STEPS)
        val lon = ((longitude.coerceIn(-180.0, 180.0) + 180.0) / 360.0 * LON_STEPS)
            .roundToLong().coerceIn(0, LON_STEPS)

        var payload = (lat shl LON_BITS) or lon
        payload = (payload shl CHECK_BITS) or checksum(lat, lon)

        return (WORD_COUNT - 1 downTo 0).map { position ->
            val index = ((payload ushr (position * BITS_PER_WORD)) and WORD_MASK).toInt()
            slots[WORD_COUNT - 1 - position][index]
        }
    }

    /** The shareable form: capitalised, with a full stop, because it is a sentence. */
    fun sentence(words: List<String>): String =
        words.joinToString(" ").replaceFirstChar { it.uppercase() } + "."

    fun sentenceFor(latitude: Double, longitude: Double): String =
        sentence(encode(latitude, longitude))

    /**
     * Find and read a code anywhere in [text].
     *
     * Shared messages carry a name and a line of explanation around the sentence, and people
     * forward the lot, so the decoder looks for the code rather than demanding the text be
     * nothing else. Punctuation and capitalisation are ignored.
     */
    fun decode(text: String): Decoded {
        val tokens = text.lowercase()
            .split(Regex("[^a-z]+"))
            .filter { it.isNotEmpty() }
        if (tokens.size < WORD_COUNT) return Decoded.NotFound

        var bestFailure: Decoded = Decoded.NotFound
        for (start in 0..tokens.size - WORD_COUNT) {
            when (val attempt = readWindow(tokens.subList(start, start + WORD_COUNT))) {
                is Decoded.Location -> return attempt
                // Keep the most informative failure: being told which word is wrong beats being
                // told nothing was found, but only if nothing later in the text decodes cleanly.
                is Decoded.UnknownWord -> if (bestFailure is Decoded.NotFound) bestFailure = attempt
                Decoded.Garbled -> bestFailure = Decoded.Garbled
                Decoded.NotFound -> Unit
            }
        }
        return bestFailure
    }

    private fun readWindow(window: List<String>): Decoded {
        var payload = 0L
        window.forEachIndexed { position, word ->
            val index = lookups[position][word] ?: return Decoded.UnknownWord(word, position)
            payload = (payload shl BITS_PER_WORD) or index.toLong()
        }
        val check = payload and CHECK_MASK
        val body = payload ushr CHECK_BITS
        val lat = body ushr LON_BITS
        val lon = body and LON_STEPS
        if (check != checksum(lat, lon)) return Decoded.Garbled
        return Decoded.Location(
            latitude = lat.toDouble() / LAT_STEPS * 180.0 - 90.0,
            longitude = lon.toDouble() / LON_STEPS * 360.0 - 180.0,
        )
    }

    /**
     * Three bits that depend on every bit of the position.
     *
     * Folding the payload down in [CHECK_BITS]-wide slices spreads every input bit into the
     * result, so the usual mistake — one word remembered wrong — almost always lands on a
     * sentence the decoder refuses rather than on a different real place.
     */
    private fun checksum(lat: Long, lon: Long): Long {
        var acc = 0L
        var remaining = (lat shl LON_BITS) or lon
        while (remaining != 0L) {
            acc = acc xor (remaining and CHECK_MASK)
            remaining = remaining ushr CHECK_BITS
        }
        return acc and CHECK_MASK
    }

    /**
     * How far a code can be from the position it was made from, worst case, at the equator. A pin
     * shared this way is never tighter than this however good the sender's fix was.
     */
    const val QUANTISATION_METERS = 2.7f

    private const val BITS_PER_WORD = 7
    private const val WORD_MASK = (1L shl BITS_PER_WORD) - 1
}
