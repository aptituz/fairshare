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

class C24ImporterTest {
    private val importer = C24Importer()

    @Test
    fun `infers c24 transaction type`() {
        val path = copyResourceToTemp("imports/c24.csv")
        assertTrue(importer.canHandle(path))
        val parsed = importer.parse(path)

        assertEquals(2, parsed.size)
        assertEquals(BigDecimal("-12.34"), parsed[0].amount)
        assertEquals("Kartenzahlung", parsed[0].transactionType)
        assertEquals("Gehalt", parsed[1].transactionType)
    }

    private fun copyResourceToTemp(resource: String): Path {
        val inputStream =
            requireNotNull(javaClass.classLoader.getResourceAsStream(resource)) {
                "Missing test resource: $resource"
            }
        return inputStream.use { stream ->
            val tempFile = Files.createTempFile("c24", ".csv")
            Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING)
            tempFile
        }
    }
}
