/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.BudgetItemSummary
import com.fairshare.dto.CategoryExpenseSummary
import com.fairshare.dto.CategoryKey
import com.fairshare.dto.MonthlySummaryResponse
import com.fairshare.dto.PersonAmountSummary
import com.fairshare.dto.PersonKey
import com.fairshare.model.BudgetItem
import com.fairshare.model.Frequency
import com.fairshare.model.Person
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class MonthlySummaryCalculator(
    private val costSplitCalculator: CostSplitCalculator,
) {
    fun calculate(
        incomeItems: List<BudgetItem>,
        expenseItems: List<BudgetItem>,
        persons: List<Person>,
    ): MonthlySummaryResponse {
        val totalHouseholdIncome = sumMonthlyAmounts(incomeItems)
        val totalHouseholdIncomeRecurring = incomeItems
            .filter { !(it.frequency == Frequency.ONE_TIME && !it.planned) }
            .let { sumMonthlyAmounts(it) }
        val totalHouseholdExpenditure = sumMonthlyAmounts(expenseItems)

        val incomeByPerson =
            incomeItems
                .groupBy { personKey(it) }
                .map { (key, items) ->
                    PersonAmountSummary(
                        personId = key.id,
                        personName = key.name,
                        monthlyAmount = sumMonthlyAmounts(items),
                    )
                }.sortedBy { it.personName.lowercase() }

        val incomeByCategory =
            incomeItems
                .groupBy { categoryKey(it) }
                .map { (key, items) ->
                    CategoryExpenseSummary(
                        categoryId = key.id,
                        categoryName = key.name,
                        monthlyAmount = sumMonthlyAmounts(items),
                    )
                }.sortedBy { it.categoryName.lowercase() }

        val incomeByBudgetItem =
            incomeItems
                .map {
                    BudgetItemSummary(
                        budgetItemId = it.id,
                        budgetItemName = it.name,
                        monthlyAmount = it.monthlyAmount(),
                        personId = it.person?.id,
                        personName = it.person?.name ?: "Gemeinsam",
                        categoryId = it.category?.id,
                        categoryName = it.category?.name ?: "Uncategorized",
                        frequency = it.frequency,
                    )
                }.sortedBy { it.budgetItemName.lowercase() }

        val expensesByBudgetItem =
            expenseItems
                .map {
                    BudgetItemSummary(
                        budgetItemId = it.id,
                        budgetItemName = it.name,
                        monthlyAmount = it.monthlyAmount(),
                        personId = it.person?.id,
                        personName = it.person?.name ?: "Gemeinsam",
                        categoryId = it.category?.id,
                        categoryName = it.category?.name ?: "Uncategorized",
                        frequency = it.frequency,
                    )
                }.sortedBy { it.budgetItemName.lowercase() }

        val expensesByPerson =
            expenseItems
                .groupBy { personKey(it) }
                .map { (key, items) ->
                    PersonAmountSummary(
                        personId = key.id,
                        personName = key.name,
                        monthlyAmount = sumMonthlyAmounts(items),
                    )
                }.sortedBy { it.personName.lowercase() }

        val expensesByCategory =
            expenseItems
                .groupBy { categoryKey(it) }
                .map { (key, items) ->
                    CategoryExpenseSummary(
                        categoryId = key.id,
                        categoryName = key.name,
                        monthlyAmount = sumMonthlyAmounts(items),
                    )
                }.sortedBy { it.categoryName.lowercase() }

        val sharedHouseholdIncomeTotal = incomeByBudgetItem
            .filter { it.personId == null }
            .fold(BigDecimal.ZERO) { acc, item -> acc.add(item.monthlyAmount) }
        val sharedHouseholdExpenditureTotal = expensesByBudgetItem
            .filter { it.personId == null }
            .fold(BigDecimal.ZERO) { acc, item -> acc.add(item.monthlyAmount) }

        val personalIncomeTotals =
            incomeByBudgetItem
                .filter { it.personId != null }
                .groupBy { it.personId }
                .mapValues { (_, items) -> items.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.monthlyAmount) } }

        val personalExpenseTotals =
            expensesByBudgetItem
                .filter { it.personId != null }
                .groupBy { it.personId }
                .mapValues { (_, items) -> items.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.monthlyAmount) } }

        val sharedHouseholdBudgetBalanceWithoutOneTimeIncome = totalHouseholdIncomeRecurring.subtract(
            sharedHouseholdExpenditureTotal
        )
        val costSplitResult = costSplitCalculator.calculate(
            persons = persons,
            personalIncomeTotals = personalIncomeTotals,
            personalExpenseTotals = personalExpenseTotals,
            sharedHouseholdIncomeTotal = sharedHouseholdIncomeTotal,
            sharedHouseholdExpenditureTotal = sharedHouseholdExpenditureTotal,
            sharedHouseholdBudgetBalanceWithoutOneTimeIncome = sharedHouseholdBudgetBalanceWithoutOneTimeIncome
        )

        return MonthlySummaryResponse(
            totalHouseholdIncome = totalHouseholdIncome,
            totalHouseholdIncomeRecurring = totalHouseholdIncomeRecurring,
            totalHouseholdExpenditure = totalHouseholdExpenditure,
            householdBudgetBalance = totalHouseholdIncome.subtract(totalHouseholdExpenditure),
            sharedHouseholdBudgetBalanceWithoutOneTimeIncome = sharedHouseholdBudgetBalanceWithoutOneTimeIncome,
            expensesByCategory = expensesByCategory,
            incomeByCategory = incomeByCategory,
            incomeByBudgetItem = incomeByBudgetItem,
            incomeByPerson = incomeByPerson,
            expensesByPerson = expensesByPerson,
            expensesByBudgetItem = expensesByBudgetItem,
            sharedHouseholdIncomeTotal = costSplitResult.sharedHouseholdIncomeTotal,
            sharedHouseholdExpenditureTotal = costSplitResult.sharedHouseholdExpenditureTotal,
            budgetPerPerson = costSplitResult.budgetPerPerson,
            costSplit = costSplitResult.costSplit,
        )
    }
}

private fun categoryKey(item: BudgetItem): CategoryKey =
    CategoryKey(
        id = item.category?.id,
        name = item.category?.name ?: "Uncategorized",
    )

private fun personKey(item: BudgetItem): PersonKey =
    PersonKey(
        id = item.person?.id,
        name = item.person?.name ?: "Gemeinsam",
    )

private fun sumMonthlyAmounts(items: Iterable<BudgetItem>): BigDecimal =
    items.fold(BigDecimal.ZERO) { acc, budgetItem -> acc.add(budgetItem.monthlyAmount()) }
