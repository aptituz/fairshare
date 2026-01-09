/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.dto

import com.fairshare.model.BudgetItemType
import com.fairshare.model.Frequency
import java.math.BigDecimal
import java.time.LocalDate

data class BudgetItemHistoryEntryResponse(
    val id: Long?,
    val name: String,
    val amount: BigDecimal,
    val type: BudgetItemType,
    val frequency: Frequency,
    val planned: Boolean,
    val categoryCorrection: Boolean,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val previousBudgetItemId: Long?,
    val rootBudgetItemId: Long?,
    val category: CategoryResponse?,
    val person: PersonResponse?,
    val isSuspension: Boolean,
    val suspensionId: Long?,
)
