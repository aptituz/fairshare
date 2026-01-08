/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.dto

import java.math.BigDecimal

data class CategoryCorrectionRequest(
    val categoryId: Long,
    val month: String,
    val actualAmount: BigDecimal,
    val personId: Long? = null
)
