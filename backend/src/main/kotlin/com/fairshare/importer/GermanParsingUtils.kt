/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.importer

import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object GermanParsingUtils {
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMANY)

    fun parseDate(value: String): LocalDate = LocalDate.parse(value.trim(), dateFormatter)

    fun parseAmount(value: String): BigDecimal {
        val normalized =
            value
                .trim()
                .replace(".", "")
                .replace(",", ".")
        return BigDecimal(normalized)
    }
}
