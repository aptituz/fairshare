/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.mapper

import com.fairshare.dto.BudgetItemDueDatesResponse
import com.fairshare.model.BudgetItem

fun BudgetItem.toDueDatesResponse(
    dueDate: String,
    dueDates: List<String>,
): BudgetItemDueDatesResponse =
    BudgetItemDueDatesResponse(
        budgetItemId = id ?: 0,
        name = name,
        amount = amount,
        type = type,
        frequency = frequency,
        dueDate = dueDate,
        startDate = startDate,
        endDate = endDate,
        category = category?.toResponse(),
        person = person?.toResponse(),
        dueDates = dueDates,
    )
