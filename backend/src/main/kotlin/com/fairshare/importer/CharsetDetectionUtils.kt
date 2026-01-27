/*
 * Copyright (C) 2026 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.importer

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

object CharsetDetectionUtils {
    private val utf8: Charset = StandardCharsets.UTF_8
    private val iso88591: Charset = Charset.forName("ISO-8859-1")
    private val utf8Bom = "\uFEFF"

    /**
     * Prefer UTF-8 when the bytes are valid UTF-8, otherwise fall back to ISO-8859-1.
     * This keeps real-world bank exports working while correctly handling UTF-8 fixtures.
     */
    fun detectCharset(bytes: ByteArray): Charset {
        val decoder =
            utf8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
        return try {
            decoder.decode(ByteBuffer.wrap(bytes))
            utf8
        } catch (_: CharacterCodingException) {
            iso88591
        }
    }

    fun decode(bytes: ByteArray): String = String(bytes, detectCharset(bytes))

    fun readLines(path: Path): List<String> {
        val bytes = Files.readAllBytes(path)
        val charset = detectCharset(bytes)
        val text = String(bytes, charset)
        return text
            .lineSequence()
            .map { it.trimEnd('\r') }
            .mapIndexed { index, line ->
                if (index == 0) {
                    stripUtf8Bom(line)
                } else {
                    line
                }
            }.toList()
    }

    private fun stripUtf8Bom(line: String): String =
        if (line.startsWith(utf8Bom)) {
            line.removePrefix(utf8Bom)
        } else {
            line
        }
}
