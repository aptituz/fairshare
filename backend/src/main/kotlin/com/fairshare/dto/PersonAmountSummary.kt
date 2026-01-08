/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.dto

import java.math.BigDecimal

data class PersonAmountSummary(
    val personId: Long?,
    val personName: String,
    val monthlyAmount: BigDecimal,
)
