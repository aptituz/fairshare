/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.dto

import java.math.BigDecimal

data class BudgetItemValueChangeRequest(
    val amount: BigDecimal,
    val startMonth: String,
    val endMonth: String?,
)
