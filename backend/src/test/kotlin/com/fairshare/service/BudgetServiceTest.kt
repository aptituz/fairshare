/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.MonthlySummaryResponse
import com.fairshare.model.BudgetItem
import com.fairshare.model.BudgetItemType
import com.fairshare.model.Category
import com.fairshare.model.Person
import com.fairshare.repo.BudgetItemRepository
import com.fairshare.repo.PersonRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@ExtendWith(MockitoExtension::class)
class BudgetServiceTest {

    @Mock
    lateinit var budgetItemRepository: BudgetItemRepository

    @Mock
    lateinit var personRepository: PersonRepository

    @Mock
    lateinit var monthlySummaryCalculator: MonthlySummaryCalculator

    @InjectMocks
    lateinit var budgetService: BudgetService

    @Test
    fun `monthlySummary should call calculator with correct data`() {
        // given
        val month = YearMonth.of(2025, 1)
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()
        val incomeItems = listOf(BudgetItem(id = 1, name = "Salary", type = BudgetItemType.INCOME, amount = BigDecimal.TEN, category = Category(1, "cat", BudgetItemType.INCOME, 1), startDate = LocalDate.now()))
        val expenseItems = listOf(BudgetItem(id = 2, name = "Rent", type = BudgetItemType.EXPENSE, amount = BigDecimal.ONE, category = Category(2, "cat", BudgetItemType.EXPENSE, 2), startDate = LocalDate.now()))
        val persons = listOf(Person(1, "Person 1"))
        val expectedResponse = MonthlySummaryResponse(
            totalHouseholdIncome = BigDecimal.TEN,
            totalHouseholdIncomeRecurring = BigDecimal.TEN,
            totalHouseholdExpenditure = BigDecimal.ONE,
            householdBudgetBalance = BigDecimal.TEN.subtract(BigDecimal.ONE),
            sharedHouseholdBudgetBalanceWithoutOneTimeIncome = BigDecimal.ZERO,
            expensesByCategory = emptyList(),
            incomeByCategory = emptyList(),
            incomeByBudgetItem = emptyList(),
            incomeByPerson = emptyList(),
            expensesByPerson = emptyList(),
            expensesByBudgetItem = emptyList(),
            sharedHouseholdIncomeTotal = BigDecimal.ZERO,
            sharedHouseholdExpenditureTotal = BigDecimal.ZERO,
            budgetPerPerson = BigDecimal.ZERO,
            costSplit = emptyList()
        )


        `when`(budgetItemRepository.findActiveForMonth(BudgetItemType.INCOME, monthStart, monthEnd)).thenReturn(incomeItems)
        `when`(budgetItemRepository.findActiveForMonth(BudgetItemType.EXPENSE, monthStart, monthEnd)).thenReturn(expenseItems)
        `when`(personRepository.findAll()).thenReturn(persons)
        `when`(monthlySummaryCalculator.calculate(incomeItems, expenseItems, persons)).thenReturn(expectedResponse)

        // when
        val result = budgetService.monthlySummary(month)

        // then
        assertEquals(expectedResponse, result)
    }
}