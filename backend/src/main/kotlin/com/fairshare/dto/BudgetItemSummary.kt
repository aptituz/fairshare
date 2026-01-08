/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.dto

import com.fairshare.model.Frequency
import java.math.BigDecimal

data class BudgetItemSummary(
    val budgetItemId: Long?,
    val budgetItemName: String,
    val monthlyAmount: BigDecimal,
    val personId: Long?,
    val personName: String,
    val categoryId: Long?,
    val categoryName: String,
    val frequency: Frequency
)
