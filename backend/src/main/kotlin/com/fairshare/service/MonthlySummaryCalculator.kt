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
        month: java.time.YearMonth,
        persons: List<Person>,
    ): MonthlySummaryResponse {
        val dueExpenses = dueExpensesForMonth(expenseItems, month)
        val totalDueExpenses = dueExpenses.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.amount) }
        val totalHouseholdIncome = sumMonthlyAmounts(incomeItems)
        val totalHouseholdIncomeRecurring = incomeItems
            .filter { !(it.frequency == Frequency.ONE_TIME && !it.planned) }
            .let { sumMonthlyAmounts(it) }
        val totalHouseholdExpenditure = sumMonthlyAmounts(expenseItems).add(totalDueExpenses)

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
            (
                expenseItems.map { it.toSummary(it.monthlyAmount()) } +
                    dueExpenses.map {
                        it.toSummary(
                            amount = it.amount,
                            nameOverride = "${it.name} (faellig)",
                            isDue = true,
                        )
                    }
                )
                .sortedBy { it.budgetItemName.lowercase() }

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
            .filter { it.personId == null && !it.isDue }
            .fold(BigDecimal.ZERO) { acc, item -> acc.add(item.monthlyAmount) }
        val sharedHouseholdDueExpensesTotal = dueExpenses
            .filter { it.person == null }
            .fold(BigDecimal.ZERO) { acc, item -> acc.add(item.amount) }
        val personalHouseholdDueExpensesTotal = dueExpenses
            .filter { it.person != null }
            .fold(BigDecimal.ZERO) { acc, item -> acc.add(item.amount) }
        val sharedHouseholdReserveShare =
            expenseItems
                .filter { it.person == null && it.frequency != Frequency.MONTHLY }
                .let { sumMonthlyAmounts(it) }
        val personalReserveTotals =
            expenseItems
                .filter { it.person?.id != null && it.frequency != Frequency.MONTHLY }
                .groupBy { it.person!!.id }
                .mapValues { (_, items) -> sumMonthlyAmounts(items) }

        val personalIncomeTotals =
            incomeByBudgetItem
                .filter { it.personId != null }
                .groupBy { it.personId }
                .mapValues { (_, items) -> items.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.monthlyAmount) } }

        val personalExpenseTotals =
            expensesByBudgetItem
                .filter { it.personId != null && !it.isDue }
                .groupBy { it.personId }
                .mapValues { (_, items) -> items.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.monthlyAmount) } }

        val sharedHouseholdBudgetBalanceWithoutOneTimeIncome = totalHouseholdIncomeRecurring.subtract(
            sharedHouseholdExpenditureTotal
        )
        val costSplitResult = costSplitCalculator.calculate(
            persons = persons,
            personalIncomeTotals = personalIncomeTotals,
            personalExpenseTotals = personalExpenseTotals,
            personalReserveTotals = personalReserveTotals,
            sharedHouseholdIncomeTotal = sharedHouseholdIncomeTotal,
            sharedHouseholdExpenditureTotal = sharedHouseholdExpenditureTotal,
            sharedHouseholdBudgetBalanceWithoutOneTimeIncome = sharedHouseholdBudgetBalanceWithoutOneTimeIncome
        )

        return MonthlySummaryResponse(
            totalHouseholdIncome = totalHouseholdIncome,
            totalHouseholdIncomeRecurring = totalHouseholdIncomeRecurring,
            totalHouseholdExpenditure = totalHouseholdExpenditure,
            totalHouseholdDueExpenses = totalDueExpenses,
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
            sharedHouseholdDueExpensesTotal = sharedHouseholdDueExpensesTotal,
            personalHouseholdDueExpensesTotal = personalHouseholdDueExpensesTotal,
            sharedHouseholdReserveShare = sharedHouseholdReserveShare,
            budgetPerPerson = costSplitResult.budgetPerPerson,
            costSplit = costSplitResult.costSplit,
        )
    }
}

private fun BudgetItem.toSummary(
    amount: BigDecimal,
    nameOverride: String? = null,
    isDue: Boolean = false,
): BudgetItemSummary =
    BudgetItemSummary(
        budgetItemId = id,
        budgetItemName = nameOverride ?: name,
        monthlyAmount = amount,
        personId = person?.id,
        personName = person?.name ?: "Gemeinsam",
        categoryId = category?.id,
        categoryName = category?.name ?: "Uncategorized",
        frequency = frequency,
        isDue = isDue,
    )

private fun dueExpensesForMonth(
    items: List<BudgetItem>,
    month: java.time.YearMonth,
): List<BudgetItem> =
    items.filter { item ->
        val dueDate = item.dueDate ?: return@filter false
        val intervalMonths =
            when (item.frequency) {
                Frequency.QUARTERLY -> 3
                Frequency.HALF_YEARLY -> 6
                Frequency.YEARLY -> 12
                else -> return@filter false
            }
        val dueMonth = java.time.YearMonth.parse(dueDate)
        val monthsBetween =
            (month.year - dueMonth.year) * 12 + (month.monthValue - dueMonth.monthValue)
        monthsBetween >= 0 && monthsBetween % intervalMonths == 0
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
