/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.exception.BadRequestException
import com.fairshare.importer.BankImporter
import com.fairshare.importer.StandardTransaction
import com.fairshare.util.HashingUtils
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.nio.file.Path
import java.time.LocalDate

@Service
class ImportHelperService(
    private val importers: List<BankImporter>,
) {
    fun detectImporter(filePath: Path): BankImporter =
        importers.firstOrNull { it.canHandle(filePath) }
            ?: throw BadRequestException("Unknown bank format")

    fun buildDescription(transaction: StandardTransaction): String {
        val parts =
            listOfNotNull(
                transaction.counterpartyName?.trim()?.takeIf { it.isNotBlank() },
                transaction.purpose?.trim()?.takeIf { it.isNotBlank() },
            )
        return parts.joinToString(" - ").ifBlank { "Unbekannte Transaktion" }
    }

    fun deduplicationKey(
        date: LocalDate,
        amount: BigDecimal,
        description: String,
    ): String = HashingUtils.sha256Hex("${date}|${amount.toPlainString()}|$description")
}
