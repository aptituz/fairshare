package com.fairshare.dto

import java.math.BigDecimal

data class CategoryExpenseSummary(
    val categoryId: Long?,
    val categoryName: String,
    val monthlyAmount: BigDecimal,
)
