/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.dto

import java.math.BigDecimal

data class MonthlyTotalsResponse(
    val month: String,
    val totalHouseholdIncome: BigDecimal,
    val totalHouseholdExpenditure: BigDecimal,
    val householdBudgetBalance: BigDecimal,
)
