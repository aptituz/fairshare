/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.importer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class TargobankImporterTest {
    private val importer = TargobankImporter()

    @Test
    fun `parses targobank rows`() {
        val path = copyResourceToTemp("imports/targobank.csv")
        assertTrue(importer.canHandle(path))
        val parsed = importer.parse(path)

        assertEquals(1, parsed.size)
        val transaction = parsed.first()
        assertEquals(BigDecimal("-123.45"), transaction.amount)
        assertEquals("Überweisung", transaction.transactionType)
        assertEquals("PVS Rhein Ruhr GmBH", transaction.counterpartyName)
        assertEquals("Rechnung 123", transaction.purpose)
        assertEquals(
            "27.01.2025;27.01.2025;Überweisung  PVS Rhein Ruhr GmBH;Rechnung 123;-123,45",
            transaction.rawLine,
        )
    }

    private fun copyResourceToTemp(resource: String): Path {
        val inputStream =
            requireNotNull(javaClass.classLoader.getResourceAsStream(resource)) {
                "Missing test resource: $resource"
            }
        return inputStream.use { stream ->
            val tempFile = Files.createTempFile("targobank", ".csv")
            Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING)
            tempFile
        }
    }
}
