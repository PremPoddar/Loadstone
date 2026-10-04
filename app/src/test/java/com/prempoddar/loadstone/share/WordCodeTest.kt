package com.prempoddar.loadstone.share

import com.google.common.truth.Truth.assertThat
import com.prempoddar.loadstone.core.geo.GeoMath
import org.junit.Test
import kotlin.random.Random

class WordCodeTest {

    private fun roundTripErrorMeters(lat: Double, lon: Double): Double {
        val decoded = WordCode.decode(WordCode.sentenceFor(lat, lon))
        assertThat(decoded).isInstanceOf(WordCode.Decoded.Location::class.java)
        val got = decoded as WordCode.Decoded.Location
        return GeoMath.distanceMeters(lat, lon, got.latitude, got.longitude)
    }

    @Test fun `the vocabularies are the size the bit maths assumes`() {
        listOf(ADJECTIVES, NOUNS, ADVERBS, VERBS).forEach { list ->
            assertThat(list).hasSize(128)
            assertThat(list.toSet()).hasSize(128)
        }
    }

    @Test fun `a code is seven words and reads as a sentence`() {
        val words = WordCode.encode(51.5074, -0.1278)
        assertThat(words).hasSize(WordCode.WORD_COUNT)
        val sentence = WordCode.sentence(words)
        assertThat(sentence.first().isUpperCase()).isTrue()
        assertThat(sentence).endsWith(".")
        assertThat(sentence.split(" ")).hasSize(WordCode.WORD_COUNT)
    }

    @Test fun `each word comes from the vocabulary its position allows`() {
        val words = WordCode.encode(-33.8688, 151.2093)
        assertThat(ADJECTIVES).contains(words[0])
        assertThat(NOUNS).contains(words[1])
        assertThat(ADVERBS).contains(words[2])
        assertThat(VERBS).contains(words[3])
        assertThat(ADJECTIVES).contains(words[4])
        assertThat(NOUNS).contains(words[5])
        assertThat(ADVERBS).contains(words[6])
    }

    @Test fun `a round trip lands within a few meters anywhere on Earth`() {
        val rng = Random(42)
        var worst = 0.0
        repeat(3000) {
            worst = maxOf(worst, roundTripErrorMeters(rng.nextDouble(-85.0, 85.0), rng.nextDouble(-180.0, 180.0)))
        }
        assertThat(worst).isLessThan(3.0)
    }

    @Test fun `the extremes survive the round trip`() {
        listOf(
            0.0 to 0.0,
            90.0 to 180.0,
            -90.0 to -180.0,
            0.0 to 179.999999,
            -89.999999 to 0.0,
        ).forEach { (lat, lon) ->
            assertThat(roundTripErrorMeters(lat, lon)).isLessThan(3.0)
        }
    }

    @Test fun `the code is found inside a forwarded message`() {
        val sentence = WordCode.sentenceFor(48.8584, 2.2945)
        val message = """
            Tower car park, level 3 by the lift
            $sentence
            Open Loadstone and tap Add from words.
        """.trimIndent()
        val decoded = WordCode.decode(message) as WordCode.Decoded.Location
        assertThat(GeoMath.distanceMeters(48.8584, 2.2945, decoded.latitude, decoded.longitude))
            .isLessThan(3.0)
    }

    @Test fun `case and punctuation do not matter`() {
        val words = WordCode.encode(35.6762, 139.6503)
        val shouty = words.joinToString(", ") { it.uppercase() } + "!!!"
        assertThat(WordCode.decode(shouty)).isEqualTo(WordCode.decode(WordCode.sentence(words)))
    }

    @Test fun `a word that belongs to no vocabulary is named`() {
        val words = WordCode.encode(1.3521, 103.8198).toMutableList()
        words[2] = "wibble"
        val result = WordCode.decode(words.joinToString(" "))
        assertThat(result).isEqualTo(WordCode.Decoded.UnknownWord("wibble", 2))
    }

    @Test fun `text with no code in it is reported as such`() {
        assertThat(WordCode.decode("meet me by the big tree")).isEqualTo(WordCode.Decoded.NotFound)
        assertThat(WordCode.decode("")).isEqualTo(WordCode.Decoded.NotFound)
    }

    @Test fun `one word remembered wrong is almost always caught`() {
        val rng = Random(9)
        var accepted = 0
        val trials = 2000
        repeat(trials) {
            val words = WordCode.encode(rng.nextDouble(-80.0, 80.0), rng.nextDouble(-180.0, 180.0)).toMutableList()
            val position = rng.nextInt(WordCode.WORD_COUNT)
            val vocabulary = when (position) {
                0, 4 -> ADJECTIVES
                1, 5 -> NOUNS
                3 -> VERBS
                else -> ADVERBS
            }
            val replacement = vocabulary[rng.nextInt(vocabulary.size)]
            if (replacement == words[position]) return@repeat
            words[position] = replacement
            if (WordCode.decode(words.joinToString(" ")) is WordCode.Decoded.Location) accepted++
        }
        // Three checksum bits can only promise about one in eight; in practice a single wrong
        // word disturbs the fold enough to do better than that.
        assertThat(accepted.toDouble() / trials).isLessThan(0.14)
    }

    @Test fun `the same position always produces the same sentence`() {
        repeat(5) {
            assertThat(WordCode.sentenceFor(55.9533, -3.1883))
                .isEqualTo(WordCode.sentenceFor(55.9533, -3.1883))
        }
    }
}
