/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.MonthlyExpenseTotalResponse
import com.fairshare.dto.MonthlySummaryResponse
import com.fairshare.dto.MonthlyTotalsResponse
import com.fairshare.dto.YearlyExpenseSummaryResponse
import com.fairshare.dto.YearlySummaryResponse
import com.fairshare.model.BudgetItemType
import com.fairshare.repo.BudgetItemRepository
import com.fairshare.repo.BudgetItemSuspensionRepository
import com.fairshare.repo.PersonRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.YearMonth

@Service
class BudgetService(
    private val budgetItemRepository: BudgetItemRepository,
    private val budgetItemSuspensionRepository: BudgetItemSuspensionRepository,
    private val personRepository: PersonRepository,
    private val monthlySummaryCalculator: MonthlySummaryCalculator,
) {
    private val log = org.slf4j.LoggerFactory.getLogger(BudgetService::class.java)

    fun monthlySummary(month: YearMonth): MonthlySummaryResponse {
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()
        log.info("Calculating monthly summary for $month")

        val incomeItems =
            budgetItemRepository.findEffectiveForMonth(
                BudgetItemType.INCOME,
                monthStart,
                monthEnd,
            )
        val expenseItems =
            budgetItemRepository.findEffectiveForMonth(
                BudgetItemType.EXPENSE,
                monthStart,
                monthEnd,
            )
        val adjustedIncomeItems = applySuspensionsForMonth(incomeItems, monthStart, monthEnd)
        val adjustedExpenseItems = applySuspensionsForMonth(expenseItems, monthStart, monthEnd)
        val persons = personRepository.findAll()
        return monthlySummaryCalculator.calculate(adjustedIncomeItems, adjustedExpenseItems, month, persons)
    }

    fun yearlySummary(year: Int): YearlySummaryResponse {
        val from = YearMonth.of(year, 1)
        val to = YearMonth.of(year, 12)
        return YearlySummaryResponse(year = year, months = monthlyTotals(from, to))
    }

    fun yearlyExpenseSummary(
        year: Int,
        personId: Long?,
    ): YearlyExpenseSummaryResponse {
        val from = YearMonth.of(year, 1)
        val to = YearMonth.of(year, 12)
        return YearlyExpenseSummaryResponse(
            year = year,
            months = monthlyExpenseTotals(from, to, personId),
        )
    }

    fun monthlyTotals(
        from: YearMonth,
        to: YearMonth,
    ): List<MonthlyTotalsResponse> {
        val months = mutableListOf<YearMonth>()
        var cursor = from
        while (!cursor.isAfter(to)) {
            months.add(cursor)
            cursor = cursor.plusMonths(1)
        }
        return months.map { month ->
            val monthStart = month.atDay(1)
            val monthEnd = month.atEndOfMonth()
            val incomeItems =
                budgetItemRepository.findEffectiveForMonth(
                    BudgetItemType.INCOME,
                    monthStart,
                    monthEnd,
                )
            val expenseItems =
                budgetItemRepository.findEffectiveForMonth(
                    BudgetItemType.EXPENSE,
                    monthStart,
                    monthEnd,
                )
            val adjustedIncomeItems = applySuspensionsForMonth(incomeItems, monthStart, monthEnd)
            val adjustedExpenseItems = applySuspensionsForMonth(expenseItems, monthStart, monthEnd)
            val totalIncome = sumMonthlyAmounts(adjustedIncomeItems)
            val dueExpenseTotal = dueExpensesForMonth(adjustedExpenseItems, month)
                .fold(BigDecimal.ZERO) { acc, item -> acc.add(item.amount) }
            val totalExpense = sumMonthlyAmounts(adjustedExpenseItems).add(dueExpenseTotal)
            MonthlyTotalsResponse(
                month = month.toString(),
                totalHouseholdIncome = totalIncome,
                totalHouseholdExpenditure = totalExpense,
                householdBudgetBalance = totalIncome.subtract(totalExpense),
            )
        }
    }

    fun monthlyExpenseTotals(
        from: YearMonth,
        to: YearMonth,
        personId: Long?,
    ): List<MonthlyExpenseTotalResponse> {
        val months = mutableListOf<YearMonth>()
        var cursor = from
        while (!cursor.isAfter(to)) {
            months.add(cursor)
            cursor = cursor.plusMonths(1)
        }
        return months.map { month ->
            val monthStart = month.atDay(1)
            val monthEnd = month.atEndOfMonth()
            val expenseItems =
                budgetItemRepository.findEffectiveForMonthByPerson(
                    BudgetItemType.EXPENSE,
                    personId,
                    monthStart,
                    monthEnd,
                )
            val adjustedExpenseItems = applySuspensionsForMonth(expenseItems, monthStart, monthEnd)
            val dueExpenseTotal = dueExpensesForMonth(adjustedExpenseItems, month)
                .fold(BigDecimal.ZERO) { acc, item -> acc.add(item.amount) }
            MonthlyExpenseTotalResponse(
                month = month.toString(),
                totalExpense = sumMonthlyAmounts(adjustedExpenseItems).add(dueExpenseTotal),
            )
        }
    }

    private fun dueExpensesForMonth(
        items: List<com.fairshare.model.BudgetItem>,
        month: YearMonth,
    ): List<com.fairshare.model.BudgetItem> =
        items.filter { item ->
            val dueDate = item.dueDate ?: return@filter false
            val intervalMonths =
                when (item.frequency) {
                    com.fairshare.model.Frequency.QUARTERLY -> 3
                    com.fairshare.model.Frequency.HALF_YEARLY -> 6
                    com.fairshare.model.Frequency.YEARLY -> 12
                    else -> return@filter false
                }
            val dueMonth = YearMonth.parse(dueDate)
            val monthsBetween =
                (month.year - dueMonth.year) * 12 + (month.monthValue - dueMonth.monthValue)
            monthsBetween >= 0 && monthsBetween % intervalMonths == 0
        }

    private fun applySuspensionsForMonth(
        items: List<com.fairshare.model.BudgetItem>,
        monthStart: java.time.LocalDate,
        monthEnd: java.time.LocalDate,
    ): List<com.fairshare.model.BudgetItem> {
        val itemIds = items.mapNotNull { it.id }
        if (itemIds.isEmpty()) {
            return items
        }
        val suspensions =
            budgetItemSuspensionRepository.findActiveForItemsAndMonth(itemIds, monthStart, monthEnd)
        val suspendedIds = suspensions.mapNotNull { it.budgetItem.id }.toSet()
        if (suspendedIds.isEmpty()) {
            return items
        }
        return items.map { item ->
            if (item.id != null && suspendedIds.contains(item.id)) {
                com.fairshare.model.BudgetItem(
                    id = item.id,
                    name = item.name,
                    amount = BigDecimal.ZERO,
                    type = item.type,
                    frequency = item.frequency,
                    planned = item.planned,
                    categoryCorrection = item.categoryCorrection,
                    startDate = item.startDate,
                    endDate = item.endDate,
                    category = item.category,
                    person = item.person,
                    previousBudgetItem = item.previousBudgetItem,
                    rootBudgetItem = item.rootBudgetItem,
                )
            } else {
                item
            }
        }
    }

    private fun sumMonthlyAmounts(items: List<com.fairshare.model.BudgetItem>): BigDecimal =
        items.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.monthlyAmount()) }
}
