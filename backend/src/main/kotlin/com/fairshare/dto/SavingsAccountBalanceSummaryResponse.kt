/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.dto

import java.math.BigDecimal

data class SavingsAccountBalanceSummaryResponse(
    val month: String,
    val totalBalance: BigDecimal,
    val expectedBalance: BigDecimal,
    val expectedMonthlySavings: BigDecimal,
)
