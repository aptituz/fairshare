/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.dto

import com.fairshare.model.BudgetItemType
import com.fairshare.model.Frequency
import java.math.BigDecimal
import java.time.LocalDate

data class DataExportPayload(
    val version: Int = 1,
    val persons: List<PersonExport>,
    val categories: List<CategoryExport>,
    val budgetItems: List<BudgetItemExport>,
    val budgetItemSuspensions: List<BudgetItemSuspensionExport>,
    val savingsAccounts: List<SavingsAccountExport>,
    val savingsAccountBalances: List<SavingsAccountBalanceExport>,
)

data class PersonExport(
    val id: Long,
    val name: String,
    val username: String,
    val passwordHash: String? = null,
    val passwordSalt: String? = null,
)

data class CategoryExport(
    val id: Long,
    val name: String,
    val type: BudgetItemType,
    val rank: Int,
)

data class BudgetItemExport(
    val id: Long,
    val name: String,
    val amount: BigDecimal,
    val type: BudgetItemType,
    val frequency: Frequency,
    val planned: Boolean,
    val categoryCorrection: Boolean,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val dueDate: String?,
    val categoryId: Long?,
    val personId: Long?,
    val previousBudgetItemId: Long?,
    val rootBudgetItemId: Long?,
)

data class BudgetItemSuspensionExport(
    val id: Long,
    val budgetItemId: Long,
    val startDate: LocalDate,
    val endDate: LocalDate?,
)

data class SavingsAccountExport(
    val id: Long,
    val name: String,
    val ownerPersonId: Long?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
)

data class SavingsAccountBalanceExport(
    val id: Long,
    val savingsAccountId: Long,
    val balanceDate: LocalDate,
    val balanceAmount: BigDecimal,
)
