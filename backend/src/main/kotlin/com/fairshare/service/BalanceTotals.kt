/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import java.math.BigDecimal

data class BalanceTotals(
    val total: BigDecimal,
    val hasAny: Boolean,
)
