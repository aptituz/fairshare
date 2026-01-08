/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.dto

import com.fairshare.model.BudgetItemType

data class CategoryResponse(
    val id: Long?,
    val name: String,
    val type: BudgetItemType,
    val rank: Int
)
