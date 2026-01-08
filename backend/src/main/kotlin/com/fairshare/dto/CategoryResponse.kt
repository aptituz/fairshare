package com.fairshare.dto

import com.fairshare.model.BudgetItemType

data class CategoryResponse(
    val id: Long?,
    val name: String,
    val type: BudgetItemType,
    val rank: Int,
)
