package com.fairshare.dto

import java.math.BigDecimal

data class PersonAmountSummary(
    val personId: Long?,
    val personName: String,
    val monthlyAmount: BigDecimal
)
