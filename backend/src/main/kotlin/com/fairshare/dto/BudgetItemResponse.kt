/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.dto

import com.fairshare.model.BudgetItemType
import com.fairshare.model.Frequency
import java.math.BigDecimal
import java.time.LocalDate

data class BudgetItemResponse(
    val id: Long?,
    val name: String,
    val amount: BigDecimal,
    val type: BudgetItemType,
    val frequency: Frequency,
    val monthlyAmount: BigDecimal,
    val planned: Boolean,
    val suspendedForMonth: Boolean = false,
    val categoryCorrection: Boolean,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val dueDate: String?,
    val nextDueMonth: String? = null,
    val previousBudgetItemId: Long?,
    val rootBudgetItemId: Long?,
    val category: CategoryResponse?,
    val person: PersonResponse?,
)
