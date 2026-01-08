/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.mapper

import com.fairshare.dto.BudgetItemResponse
import com.fairshare.model.BudgetItem

fun BudgetItem.toResponse(): BudgetItemResponse =
    BudgetItemResponse(
        id = id,
        name = name,
        amount = amount,
        type = type,
        frequency = frequency,
        monthlyAmount = monthlyAmount(),
        planned = planned,
        suspendedForMonth = false,
        categoryCorrection = categoryCorrection,
        startDate = startDate,
        endDate = endDate,
        previousBudgetItemId = previousBudgetItem?.id,
        rootBudgetItemId = rootBudgetItem?.id,
        category = category?.toResponse(),
        person = person?.toResponse(),
    )
