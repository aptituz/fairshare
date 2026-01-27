/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.importer

import java.math.BigDecimal
import java.time.LocalDate

data class StandardTransaction(
    val bookingDate: LocalDate,
    val valueDate: LocalDate?,
    val amount: BigDecimal,
    val counterpartyName: String?,
    val purpose: String?,
    val transactionType: String?,
    val rawLine: String,
)
