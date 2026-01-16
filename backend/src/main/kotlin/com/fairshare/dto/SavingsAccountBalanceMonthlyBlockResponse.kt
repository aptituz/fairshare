/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.dto

import java.math.BigDecimal

data class SavingsAccountBalanceMonthlyBlockResponse(
    val month: String,
    val balances: List<SavingsAccountBalanceMonthlyEntryResponse>,
    val totalBefore: BigDecimal,
    val totalAfter: BigDecimal,
    val totalDifference: BigDecimal,
    val hasBefore: Boolean,
    val isEstimated: Boolean,
)
