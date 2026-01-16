/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.dto

import java.math.BigDecimal

data class PersonCostSplitResponse(
    val personId: Long?,
    val name: String,
    val personalIncome: BigDecimal,
    val personalExpenses: BigDecimal,
    val personalReserveShare: BigDecimal,
    val personalUsableIncome: BigDecimal,
    val personalCostShare: BigDecimal,
    val personalContribution: BigDecimal,
)
