/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.importer

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path

@Component
class TargobankImporter : BankImporter {
    private val headerPrefix = "Buchungstag;Wertstellung;Vorgang"
    private val amountRegex =
        Regex("^-?\\d{1,3}(\\.\\d{3})*,\\d{2}$|^-?\\d+,\\d{2}$")
    private val informationalKeywords =
        setOf(
            "zins",
            "zinssatz",
            "zinsen",
            "sollzins",
            "haben",
            "effektivzins",
            "info",
            "hinweis",
        )

    override fun canHandle(filePath: Path): Boolean =
        Files.readAllBytes(filePath).let { bytes ->
            val charset = CharsetDetectionUtils.detectCharset(bytes)
            val firstLine =
                String(bytes, charset)
                    .lineSequence()
                    .firstOrNull { it.isNotBlank() }
                    ?.trimEnd('\r')
                    ?: return@let false
            if (firstLine.contains(headerPrefix)) return@let true

            val columns = CsvParsingUtils.parseLine(firstLine, ';')
            if (columns.size < 3) return@let false

            val hasDate =
                try {
                    GermanParsingUtils.parseDate(columns.first())
                    true
                } catch (_: Exception) {
                    false
                }
            val hasAmount = columns.drop(1).any { amountRegex.matches(it.trim()) }
            hasDate && hasAmount
        }

    override fun parse(filePath: Path): List<StandardTransaction> {
        val lines = CharsetDetectionUtils.readLines(filePath)
        if (lines.isEmpty()) return emptyList()
        val firstLine = lines.first()
        val headerColumns = CsvParsingUtils.parseLine(firstLine, ';').map { it.trim() }
        val headerIndexMap = headerColumns.withIndex().associate { it.value to it.index }
        val hasHeader =
            listOf("Buchungstag", "Wertstellung", "Vorgang", "Buchungstext", "Betrag")
                .all { it in headerIndexMap }

        val bookingIndex = headerIndexMap["Buchungstag"] ?: 0
        val valueIndex = headerIndexMap["Wertstellung"] ?: bookingIndex
        val vorgangIndex = headerIndexMap["Vorgang"] ?: 1
        val textIndex = headerIndexMap["Buchungstext"] ?: -1
        val amountIndex = headerIndexMap["Betrag"] ?: 2

        val dataLines = if (hasHeader) lines.drop(1) else lines

        return dataLines.mapNotNull { line ->
            if (line.isBlank()) return@mapNotNull null
            val columns = CsvParsingUtils.parseLine(line, ';')
            val effectiveAmountIndex =
                if (hasHeader) {
                    amountIndex
                } else {
                    findAmountIndex(columns) ?: return@mapNotNull null
                }
            if (columns.size <= effectiveAmountIndex) return@mapNotNull null
            val bookingDate =
                columns.getOrNull(bookingIndex)?.let { value ->
                    runCatching { GermanParsingUtils.parseDate(value) }.getOrNull()
                } ?: return@mapNotNull null
            val valueDate =
                columns.getOrNull(valueIndex)?.let { value ->
                    runCatching { GermanParsingUtils.parseDate(value) }.getOrNull()
                } ?: bookingDate
            val vorgang =
                if (hasHeader) {
                    columns.getOrNull(vorgangIndex)?.trim().orEmpty()
                } else {
                    columns
                        .slice(vorgangIndex until effectiveAmountIndex)
                        .joinToString(";")
                        .trim()
                }
            val vorgangParts = vorgang.split(Regex("\\s{2,}"), limit = 2)
            val transactionType = vorgangParts.firstOrNull()?.trim()?.ifBlank { null }
            val counterparty = vorgangParts.getOrNull(1)?.trim()?.ifBlank { null }
            val purpose =
                columns.getOrNull(textIndex)?.trim()?.ifBlank { null }
            val amount = GermanParsingUtils.parseAmount(columns[effectiveAmountIndex])
            if (isInformational(vorgang, amount)) return@mapNotNull null
            StandardTransaction(
                bookingDate = bookingDate,
                valueDate = valueDate,
                amount = amount,
                counterpartyName = counterparty,
                purpose = purpose,
                transactionType = transactionType,
                rawLine = line,
            )
        }
    }

    override fun getBankName(): String = "targobank"

    private fun findAmountIndex(columns: List<String>): Int? {
        val upperBound = minOf(columns.lastIndex, 4)
        for (index in 1..upperBound) {
            if (amountRegex.matches(columns[index].trim())) return index
        }
        return null
    }

    private fun isInformational(
        vorgang: String,
        amount: BigDecimal,
    ): Boolean {
        if (amount.compareTo(BigDecimal.ZERO) != 0) return false
        val normalized = vorgang.lowercase()
        return informationalKeywords.any { it in normalized }
    }
}
