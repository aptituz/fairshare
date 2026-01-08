/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.dto

import java.math.BigDecimal

data class MonthlySummaryResponse(
    val totalHouseholdIncome: BigDecimal,
    val totalHouseholdIncomeRecurring: BigDecimal,
    val totalHouseholdExpenditure: BigDecimal,
    val householdBudgetBalance: BigDecimal,
    val sharedHouseholdBudgetBalanceWithoutOneTimeIncome: BigDecimal,
    val expensesByCategory: List<CategoryExpenseSummary>,
    val incomeByCategory: List<CategoryExpenseSummary>,
    val incomeByBudgetItem: List<BudgetItemSummary>,
    val incomeByPerson: List<PersonAmountSummary>,
    val expensesByPerson: List<PersonAmountSummary>,
    val expensesByBudgetItem: List<BudgetItemSummary>,
    val sharedHouseholdIncomeTotal: BigDecimal,
    val sharedHouseholdExpenditureTotal: BigDecimal,
    val budgetPerPerson: BigDecimal,
    val costSplit: List<PersonCostSplitResponse>,
)
