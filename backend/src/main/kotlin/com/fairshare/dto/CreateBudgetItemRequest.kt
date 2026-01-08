package com.fairshare.dto

import com.fairshare.model.BudgetItemType
import com.fairshare.model.Frequency
import java.math.BigDecimal
import java.time.LocalDate

data class CreateBudgetItemRequest(
    val name: String,
    val amount: BigDecimal,
    val type: BudgetItemType,
    val categoryId: Long?,
    val personId: Long? = null,
    val frequency: Frequency? = null,
    val active: Boolean? = null,
    val planned: Boolean? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
)
