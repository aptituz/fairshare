package com.fairshare.dto

import java.math.BigDecimal

data class PersonCostSplitResponse(
    val personId: Long?,
    val name: String,
    val personalIncome: BigDecimal,
    val personalExpenses: BigDecimal,
    val personalUsableIncome: BigDecimal,
    val personalCostShare: BigDecimal,
    val personalContribution: BigDecimal
)
