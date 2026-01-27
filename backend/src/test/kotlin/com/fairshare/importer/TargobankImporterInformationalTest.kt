/*
 * Copyright (C) 2026 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.importer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Files

class TargobankImporterInformationalTest {
    private val importer = TargobankImporter()

    @Test
    fun `ignores informational interest-rate rows with zero amount`() {
        val csv =
            """
            15.01.2025;Kartenzahlung  Coffee Shop;-5,00;;;;'DE001'
            16.01.2025;Info Sollzinsänderung;0,00;;;;'DE001'
            17.01.2025;Lastschrift  Utility Company;-12,34;;;;'DE001'
            """.trimIndent()

        val tempFile = Files.createTempFile("targobank-informational", ".csv")
        Files.writeString(tempFile, csv)

        val parsed = importer.parse(tempFile)

        assertEquals(2, parsed.size)
    }
}
