/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.MonthlySummaryResponse
import com.fairshare.model.BudgetItemType
import com.fairshare.repo.BudgetItemRepository
import com.fairshare.repo.PersonRepository
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class BudgetService(
    private val budgetItemRepository: BudgetItemRepository,
    private val personRepository: PersonRepository,
    private val monthlySummaryCalculator: MonthlySummaryCalculator
) {
    private val log = org.slf4j.LoggerFactory.getLogger(BudgetService::class.java)

    fun monthlySummary(month: YearMonth): MonthlySummaryResponse {
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()
        log.info("Calculating monthly summary for $month")

        val incomeItems = budgetItemRepository.findActiveForMonth(
            BudgetItemType.INCOME,
            monthStart,
            monthEnd
        )
        val expenseItems = budgetItemRepository.findActiveForMonth(
            BudgetItemType.EXPENSE,
            monthStart,
            monthEnd
        )
        val persons = personRepository.findAll()
        return monthlySummaryCalculator.calculate(incomeItems, expenseItems, persons)
    }
}
