package com.fairshare.dto

import com.fairshare.model.BudgetItemType

data class CreateCategoryRequest(
    val name: String,
    val type: BudgetItemType
)
