/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.importer

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

@Component
class C24Importer : BankImporter {
    override fun canHandle(filePath: Path): Boolean =
        Files.newBufferedReader(filePath, StandardCharsets.UTF_8).use { reader ->
            val header = reader.readLine() ?: return@use false
            header.contains("Buchungsdatum") && header.contains("Auftraggeber/Empfänger")
        }

    override fun parse(filePath: Path): List<StandardTransaction> {
        val lines = Files.newBufferedReader(filePath, StandardCharsets.UTF_8).use { it.readLines() }
        if (lines.isEmpty()) return emptyList()
        val headerColumns = CsvParsingUtils.parseLine(lines.first(), ';').map { it.trim() }
        val indexMap = headerColumns.withIndex().associate { it.value to it.index }
        val bookingIndex = indexMap["Buchungsdatum"] ?: return emptyList()
        val valueIndex = indexMap["Wertstellung"] ?: return emptyList()
        val purposeIndex = indexMap["Verwendungszweck"] ?: return emptyList()
        val amountIndex = indexMap["Betrag"] ?: return emptyList()
        val counterpartyIndex = indexMap["Auftraggeber/Empfänger"] ?: return emptyList()

        return lines.drop(1).mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val columns = CsvParsingUtils.parseLine(line, ';')
            if (columns.size <= amountIndex) return@mapNotNull null
            val amount = GermanParsingUtils.parseAmount(columns[amountIndex])
            val purpose = columns[purposeIndex].trim().ifBlank { null }
            val transactionType = inferTransactionType(amount, purpose)
            StandardTransaction(
                bookingDate = GermanParsingUtils.parseDate(columns[bookingIndex]),
                valueDate = GermanParsingUtils.parseDate(columns[valueIndex]),
                amount = amount,
                counterpartyName = columns[counterpartyIndex].trim().ifBlank { null },
                purpose = purpose,
                transactionType = transactionType,
                rawLine = line,
            )
        }
    }

    override fun getBankName(): String = "c24"

    private fun inferTransactionType(
        amount: BigDecimal,
        purpose: String?,
    ): String {
        val purposeValue = purpose?.lowercase().orEmpty()
        return if (amount < BigDecimal.ZERO) {
            when {
                purposeValue.contains("lastschrift") -> "Lastschrift"
                purposeValue.contains("kartenzahlung") -> "Kartenzahlung"
                purposeValue.contains("karte") -> "Kartenzahlung"
                else -> "Überweisung"
            }
        } else {
            when {
                purposeValue.contains("gehalt") -> "Gehalt"
                purposeValue.contains("lohn") -> "Lohn"
                else -> "Gutschrift"
            }
        }
    }
}
