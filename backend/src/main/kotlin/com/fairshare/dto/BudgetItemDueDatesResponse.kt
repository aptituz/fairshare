/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.dto

import com.fairshare.model.BudgetItemType
import com.fairshare.model.Frequency
import java.math.BigDecimal
import java.time.LocalDate

data class BudgetItemDueDatesResponse(
    val budgetItemId: Long,
    val name: String,
    val amount: BigDecimal,
    val type: BudgetItemType,
    val frequency: Frequency,
    val dueDate: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val category: CategoryResponse?,
    val person: PersonResponse?,
    val dueDates: List<String>,
)
