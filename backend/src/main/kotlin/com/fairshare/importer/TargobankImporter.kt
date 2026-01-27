/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.importer

import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

@Component
class TargobankImporter : BankImporter {
    override fun canHandle(filePath: Path): Boolean =
        Files.readAllBytes(filePath).let { bytes ->
            val charset = CharsetDetectionUtils.detectCharset(bytes)
            String(bytes, charset)
                .lineSequence()
                .firstOrNull()
                ?.trimEnd('\r')
                ?.contains("Buchungstag;Wertstellung;Vorgang") == true
        }

    override fun parse(filePath: Path): List<StandardTransaction> {
        val bytes = Files.readAllBytes(filePath)
        val charset = CharsetDetectionUtils.detectCharset(bytes)
        val lines =
            String(bytes, charset)
                .lineSequence()
                .map { it.trimEnd('\r') }
                .toList()
        if (lines.isEmpty()) return emptyList()
        val header = CsvParsingUtils.parseLine(lines.first(), ';').map { it.trim() }
        val indexMap = header.withIndex().associate { it.value to it.index }
        val bookingIndex = indexMap["Buchungstag"] ?: return emptyList()
        val valueIndex = indexMap["Wertstellung"] ?: return emptyList()
        val vorgangIndex = indexMap["Vorgang"] ?: return emptyList()
        val textIndex = indexMap["Buchungstext"] ?: return emptyList()
        val amountIndex = indexMap["Betrag"] ?: return emptyList()

        return lines.drop(1).mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val columns = CsvParsingUtils.parseLine(line, ';')
            if (columns.size <= amountIndex) return@mapNotNull null
            val vorgang = columns[vorgangIndex].trim()
            val vorgangParts = vorgang.split(Regex("\\s{2,}"), limit = 2)
            val transactionType = vorgangParts.firstOrNull()?.trim()?.ifBlank { null }
            val counterparty = vorgangParts.getOrNull(1)?.trim()?.ifBlank { null }
            StandardTransaction(
                bookingDate = GermanParsingUtils.parseDate(columns[bookingIndex]),
                valueDate = GermanParsingUtils.parseDate(columns[valueIndex]),
                amount = GermanParsingUtils.parseAmount(columns[amountIndex]),
                counterpartyName = counterparty,
                purpose = columns[textIndex].trim().ifBlank { null },
                transactionType = transactionType,
                rawLine = line,
            )
        }
    }

    override fun getBankName(): String = "targobank"
}
