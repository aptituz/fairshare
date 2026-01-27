/*
 * Copyright (C) 2026 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.importer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.nio.file.Files

class TargobankImporterHeaderlessTest {
    private val importer = TargobankImporter()

    @Test
    fun `parses headerless rows when amount is in third column`() {
        val csv =
            """
            15.01.2025;Lastschrift  Example Merchant;-29,30;;;;'DE001'
            """.trimIndent()

        val tempFile = Files.createTempFile("targobank-headerless", ".csv")
        Files.writeString(tempFile, csv)

        assertTrue(importer.canHandle(tempFile))
        val parsed = importer.parse(tempFile)

        assertEquals(1, parsed.size)
        assertEquals(BigDecimal("-29.30"), parsed.first().amount)
        assertEquals("Lastschrift", parsed.first().transactionType)
        assertEquals("Example Merchant", parsed.first().counterpartyName)
    }

    @Test
    fun `parses headerless rows when amount is in fourth column`() {
        val csv =
            """
            20.01.2025;Kartenzahlung  La Pizza;Il Desperato;-69,00;;;;'DE001'
            """.trimIndent()

        val tempFile = Files.createTempFile("targobank-headerless-shift", ".csv")
        Files.writeString(tempFile, csv)

        assertTrue(importer.canHandle(tempFile))
        val parsed = importer.parse(tempFile)

        assertEquals(1, parsed.size)
        assertEquals(BigDecimal("-69.00"), parsed.first().amount)
        assertEquals("Kartenzahlung", parsed.first().transactionType)
        assertEquals("La Pizza;Il Desperato", parsed.first().counterpartyName)
    }
}
