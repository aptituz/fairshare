package com.fairshare.dto

import java.math.BigDecimal

data class MonthlySummaryResponse(
    val totalIncome: BigDecimal,
    val totalIncomeRecurring: BigDecimal,
    val totalExpenses: BigDecimal,
    val netResult: BigDecimal,
    val netResultShared: BigDecimal,
    val expensesByCategory: List<CategoryExpenseSummary>,
    val incomeByCategory: List<CategoryExpenseSummary>,
    val incomeByBudgetItem: List<BudgetItemSummary>,
    val incomeByPerson: List<PersonAmountSummary>,
    val expensesByPerson: List<PersonAmountSummary>,
    val expensesByBudgetItem: List<BudgetItemSummary>,
    val sharedIncomeTotal: BigDecimal,
    val sharedExpenseTotal: BigDecimal,
    val budgetPerPerson: BigDecimal,
    val costSplit: List<PersonCostSplitResponse>
)
