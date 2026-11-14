package com.careerlens.app.domain.ats

import java.io.InputStream
import java.text.Normalizer

/**
 * Pure-Kotlin, 100% offline BERT WordPiece tokenizer matching HuggingFace `BertTokenizer`
 * (`do_lower_case = true` for `TaylorAI/bge-micro-v2` and `all-MiniLM-L6-v2`).
 */
data class EncodedTextBatch(
    val inputIds: LongArray,
    val attentionMask: LongArray,
    val tokenTypeIds: LongArray,
    val batchSize: Int,
    val seqLength: Int
)

class BertWordPieceTokenizer(
    private val vocab: Map<String, Int>,
    private val doLowerCase: Boolean = true,
    private val clsTokenId: Int = 101,
    private val sepTokenId: Int = 102,
    private val padTokenId: Int = 0,
    private val unkTokenId: Int = 100,
    val maxSeqLength: Int = 128
) {

    fun encode(text: String, maxLength: Int = maxSeqLength): IntArray {
        val wordPieces = mutableListOf<Int>()
        val budget = (maxLength - 2).coerceAtLeast(1)

        for (token in basicTokenize(text)) {
            if (wordPieces.size >= budget) break
            val pieces = wordPieceTokenize(token)
            for (pid in pieces) {
                if (wordPieces.size >= budget) break
                wordPieces.add(pid)
            }
        }

        val result = IntArray(wordPieces.size + 2)
        result[0] = clsTokenId
        for (i in wordPieces.indices) {
            result[i + 1] = wordPieces[i]
        }
        result[result.lastIndex] = sepTokenId
        return result
    }

    /**
     * Tokenizes a batch of texts and pads dynamically up to the longest item in the batch
     * (bounded between [minPadLength] and [maxLength]), avoiding wasted O(L^2) attention work.
     */
    fun encodeBatch(
        texts: List<String>,
        maxLength: Int = maxSeqLength,
        minPadLength: Int = 8
    ): EncodedTextBatch {
        if (texts.isEmpty()) {
            return EncodedTextBatch(LongArray(0), LongArray(0), LongArray(0), 0, 0)
        }
        val encodedList = texts.map { encode(it, maxLength) }
        val targetLen = (encodedList.maxOf { it.size }).coerceIn(minPadLength.coerceAtMost(maxLength), maxLength)
        val batchSize = encodedList.size
        val total = batchSize * targetLen

        val inputIds = LongArray(total) { padTokenId.toLong() }
        val attentionMask = LongArray(total) { 0L }
        val tokenTypeIds = LongArray(total) { 0L }

        encodedList.forEachIndexed { row, seq ->
            val rowOffset = row * targetLen
            val copyLen = seq.size.coerceAtMost(targetLen)
            for (col in 0 until copyLen) {
                inputIds[rowOffset + col] = seq[col].toLong()
                attentionMask[rowOffset + col] = 1L
            }
        }

        return EncodedTextBatch(
            inputIds = inputIds,
            attentionMask = attentionMask,
            tokenTypeIds = tokenTypeIds,
            batchSize = batchSize,
            seqLength = targetLen
        )
    }

    private fun basicTokenize(raw: String): List<String> {
        val cleaned = cleanControlCharsAndSpaceCjk(raw)
        val normalized = if (doLowerCase) stripAccents(cleaned.lowercase()) else cleaned
        val whitespaceTokens = normalized.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

        val splitTokens = mutableListOf<String>()
        for (tok in whitespaceTokens) {
            var current = StringBuilder()
            for (ch in tok) {
                if (isBertPunctuation(ch)) {
                    if (current.isNotEmpty()) {
                        splitTokens.add(current.toString())
                        current = StringBuilder()
                    }
                    splitTokens.add(ch.toString())
                } else {
                    current.append(ch)
                }
            }
            if (current.isNotEmpty()) {
                splitTokens.add(current.toString())
            }
        }
        return splitTokens
    }

    private fun wordPieceTokenize(token: String): List<Int> {
        if (token.length > MAX_INPUT_CHARS_PER_WORD) {
            return listOf(unkTokenId)
        }
        val outputIds = mutableListOf<Int>()
        var start = 0
        var isBad = false

        while (start < token.length) {
            var end = token.length
            var curSubId: Int? = null
            while (start < end) {
                val rawSub = token.substring(start, end)
                val lookup = if (start > 0) "##$rawSub" else rawSub
                val foundId = vocab[lookup]
                if (foundId != null) {
                    curSubId = foundId
                    break
                }
                end--
            }
            if (curSubId == null) {
                isBad = true
                break
            }
            outputIds.add(curSubId)
            start = end
        }

        return if (isBad) listOf(unkTokenId) else outputIds
    }

    private fun cleanControlCharsAndSpaceCjk(text: String): String {
        val sb = StringBuilder(text.length + 16)
        for (ch in text) {
            val code = ch.code
            if (code == 0 || code == 0xFFFD || isControlChar(ch)) {
                if (ch == '\t' || ch == '\n' || ch == '\r') {
                    sb.append(' ')
                }
                continue
            }
            if (Character.isWhitespace(ch)) {
                sb.append(' ')
            } else if (isCjkChar(code)) {
                sb.append(' ').append(ch).append(' ')
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun stripAccents(text: String): String {
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        val sb = StringBuilder(decomposed.length)
        for (ch in decomposed) {
            if (Character.getType(ch) != Character.NON_SPACING_MARK.toInt()) {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun isControlChar(ch: Char): Boolean {
        if (ch == '\t' || ch == '\n' || ch == '\r') return false
        val type = Character.getType(ch)
        return type == Character.CONTROL.toInt() || type == Character.FORMAT.toInt()
    }

    private fun isBertPunctuation(ch: Char): Boolean {
        val cp = ch.code
        if ((cp in 33..47) || (cp in 58..64) || (cp in 91..96) || (cp in 123..126)) {
            return true
        }
        return when (Character.getType(ch).toByte()) {
            Character.CONNECTOR_PUNCTUATION,
            Character.DASH_PUNCTUATION,
            Character.START_PUNCTUATION,
            Character.END_PUNCTUATION,
            Character.INITIAL_QUOTE_PUNCTUATION,
            Character.FINAL_QUOTE_PUNCTUATION,
            Character.OTHER_PUNCTUATION -> true
            else -> false
        }
    }

    private fun isCjkChar(cp: Int): Boolean {
        return (cp in 0x4E00..0x9FFF) ||
            (cp in 0x3400..0x4DBF) ||
            (cp in 0x20000..0x2A6DF) ||
            (cp in 0xF900..0xFAFF)
    }

    companion object {
        private const val MAX_INPUT_CHARS_PER_WORD = 100

        fun fromVocabStream(
            stream: InputStream,
            doLowerCase: Boolean = true,
            clsTokenId: Int = 101,
            sepTokenId: Int = 102,
            padTokenId: Int = 0,
            unkTokenId: Int = 100,
            maxSeqLength: Int = 128
        ): BertWordPieceTokenizer {
            val map = HashMap<String, Int>(32768)
            stream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                var index = 0
                lines.forEach { line ->
                    // Preserve leading/trailing hashes; trim trailing CR/LF only
                    val token = line.trimEnd('\r', '\n')
                    map[token] = index++
                }
            }
            return BertWordPieceTokenizer(
                vocab = map,
                doLowerCase = doLowerCase,
                clsTokenId = map["[CLS]"] ?: clsTokenId,
                sepTokenId = map["[SEP]"] ?: sepTokenId,
                padTokenId = map["[PAD]"] ?: padTokenId,
                unkTokenId = map["[UNK]"] ?: unkTokenId,
                maxSeqLength = maxSeqLength
            )
        }
    }
}
