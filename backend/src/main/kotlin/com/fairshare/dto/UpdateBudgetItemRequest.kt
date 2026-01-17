/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.dto

import com.fairshare.model.Frequency
import java.math.BigDecimal
import java.time.LocalDate

data class UpdateBudgetItemRequest(
    val name: String,
    val amount: BigDecimal,
    val categoryId: Long? = null,
    val personId: Long? = null,
    val frequency: Frequency? = null,
    val planned: Boolean? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val dueDate: String? = null,
)
