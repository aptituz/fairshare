/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.MonthlySummaryResponse
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
        return monthlySummaryCalculator.calculate(adjustedIncomeItems, adjustedExpenseItems, persons)
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
}
