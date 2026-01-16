/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.dto

import java.math.BigDecimal

data class SavingsAccountBalanceMonthResponse(
    val month: String,
    val totalBalance: BigDecimal,
    val expectedMonthlySavings: BigDecimal,
    val actualMonthlySavings: BigDecimal?,
    val balances: List<SavingsAccountBalanceResponse>,
)
