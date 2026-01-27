/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.importer

import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

@Component
class DkbImporter : BankImporter {
    override fun canHandle(filePath: Path): Boolean {
        val bytes = Files.readAllBytes(filePath)
        val charset = CharsetDetectionUtils.detectCharset(bytes)
        val sample = String(bytes.take(500).toByteArray(), charset)
        return sample.contains("Kontonummer:") || sample.contains("Deutsche Kreditbank")
    }

    override fun parse(filePath: Path): List<StandardTransaction> {
        val lines = CharsetDetectionUtils.readLines(filePath)
        val headerIndex = lines.indexOfFirst { it.contains("Buchungstag") }
        if (headerIndex == -1) return emptyList()
        val headerColumns = CsvParsingUtils.parseLine(lines[headerIndex], ';').map { it.trim() }
        val indexMap = headerColumns.withIndex().associate { it.value to it.index }
        val bookingIndex = indexMap["Buchungstag"] ?: return emptyList()
        val valueIndex = indexMap["Wertstellung"] ?: return emptyList()
        val typeIndex = indexMap["Buchungstext"] ?: return emptyList()
        val counterpartyIndex = indexMap["Auftraggeber / Begünstigter"] ?: return emptyList()
        val purposeIndex = indexMap["Verwendungszweck"] ?: return emptyList()
        val amountIndex = indexMap["Betrag (EUR)"] ?: indexMap["Betrag (EUR )"] ?: return emptyList()

        return lines.drop(headerIndex + 1).mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val columns = CsvParsingUtils.parseLine(line, ';')
            if (columns.size <= amountIndex) return@mapNotNull null
            StandardTransaction(
                bookingDate = GermanParsingUtils.parseDate(columns[bookingIndex]),
                valueDate = GermanParsingUtils.parseDate(columns[valueIndex]),
                amount = GermanParsingUtils.parseAmount(columns[amountIndex]),
                counterpartyName = columns[counterpartyIndex].trim().ifBlank { null },
                purpose = columns[purposeIndex].trim().ifBlank { null },
                transactionType = columns[typeIndex].trim().ifBlank { null },
                rawLine = line,
            )
        }
    }

    override fun getBankName(): String = "dkb"
}
