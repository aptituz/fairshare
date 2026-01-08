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
        active = active,
        planned = planned,
        categoryCorrection = categoryCorrection,
        startDate = startDate,
        endDate = endDate,
        category = category?.toResponse(),
        person = person?.toResponse(),
    )
