/*
 * Copyright (C) 2026 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.exception.BadRequestException
import com.fairshare.importer.BankImporter
import com.fairshare.importer.StandardTransaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate

class ImportHelperServiceTest {
    @Test
    fun `detectImporter ignores importer exceptions`() {
        val tempFile = Files.createTempFile("import-helper", ".csv")
        Files.writeString(tempFile, "dummy")

        val throwingImporter =
            object : BankImporter {
                override fun canHandle(filePath: Path): Boolean = throw IllegalStateException("boom")

                override fun parse(filePath: Path): List<StandardTransaction> = emptyList()

                override fun getBankName(): String = "throwing"
            }

        val matchingImporter =
            object : BankImporter {
                override fun canHandle(filePath: Path): Boolean = true

                override fun parse(filePath: Path): List<StandardTransaction> =
                    listOf(
                        StandardTransaction(
                            bookingDate = LocalDate.of(2025, 1, 1),
                            valueDate = LocalDate.of(2025, 1, 1),
                            amount = BigDecimal.ONE,
                            counterpartyName = null,
                            purpose = null,
                            transactionType = null,
                            rawLine = "",
                        ),
                    )

                override fun getBankName(): String = "matching"
            }

        val service = ImportHelperService(listOf(throwingImporter, matchingImporter))

        val detected = service.detectImporter(tempFile)

        assertEquals("matching", detected.getBankName())
    }

    @Test
    fun `detectImporter throws when no importer matches`() {
        val tempFile = Files.createTempFile("import-helper-none", ".csv")
        Files.writeString(tempFile, "dummy")

        val nonMatchingImporter =
            object : BankImporter {
                override fun canHandle(filePath: Path): Boolean = false

                override fun parse(filePath: Path): List<StandardTransaction> = emptyList()

                override fun getBankName(): String = "non-matching"
            }

        val service = ImportHelperService(listOf(nonMatchingImporter))

        assertThrows(BadRequestException::class.java) {
            service.detectImporter(tempFile)
        }
    }
}
