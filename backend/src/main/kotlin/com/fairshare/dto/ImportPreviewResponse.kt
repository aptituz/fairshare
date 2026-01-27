/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.dto

import java.math.BigDecimal
import java.time.LocalDate

data class ImportPreviewResponse(
    val bank: String,
    val totalCount: Int,
    val preview: List<TransactionPreviewResponse>,
)

data class TransactionPreviewResponse(
    val date: LocalDate,
    val amount: BigDecimal,
    val description: String,
)
