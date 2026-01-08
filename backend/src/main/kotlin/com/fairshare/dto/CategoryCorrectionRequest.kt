package com.fairshare.dto

import java.math.BigDecimal

data class CategoryCorrectionRequest(
    val categoryId: Long,
    val month: String,
    val actualAmount: BigDecimal,
    val personId: Long? = null,
)
