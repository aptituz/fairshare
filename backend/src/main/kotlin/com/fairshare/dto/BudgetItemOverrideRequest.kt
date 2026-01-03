package com.fairshare.dto

import java.math.BigDecimal

data class BudgetItemOverrideRequest(
    val month: String,
    val amount: BigDecimal
)
