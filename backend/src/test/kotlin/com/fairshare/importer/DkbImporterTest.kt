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

class DkbImporterTest {
    private val importer = DkbImporter()

    @Test
    fun `parses dkb rows after metadata`() {
        val path = copyResourceToTemp("imports/dkb.csv")
        assertTrue(importer.canHandle(path))
        val parsed = importer.parse(path)

        assertEquals(1, parsed.size)
        val transaction = parsed.first()
        assertEquals(BigDecimal("1234.56"), transaction.amount)
        assertEquals("Überweisung", transaction.transactionType)
        assertEquals("ACME GmbH", transaction.counterpartyName)
        assertEquals("Rechnung 123", transaction.purpose)
    }

    private fun copyResourceToTemp(resource: String): Path {
        val inputStream =
            requireNotNull(javaClass.classLoader.getResourceAsStream(resource)) {
                "Missing test resource: $resource"
            }
        return inputStream.use { stream ->
            val tempFile = Files.createTempFile("dkb", ".csv")
            Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING)
            tempFile
        }
    }
}
