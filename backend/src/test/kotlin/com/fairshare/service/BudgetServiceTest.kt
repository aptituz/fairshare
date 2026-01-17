/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.MonthlySummaryResponse
import com.fairshare.model.BudgetItem
import com.fairshare.model.BudgetItemType
import com.fairshare.model.Category
import com.fairshare.model.Frequency
import com.fairshare.model.Person
import com.fairshare.repo.BudgetItemRepository
import com.fairshare.repo.BudgetItemSuspensionRepository
import com.fairshare.repo.PersonRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.doAnswer
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@ExtendWith(MockitoExtension::class)
class BudgetServiceTest {

    @Mock
    lateinit var budgetItemRepository: BudgetItemRepository

    @Mock
    lateinit var budgetItemSuspensionRepository: BudgetItemSuspensionRepository

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
        val persons = listOf(Person(1, "Person 1", "person1"))
        val expectedResponse = MonthlySummaryResponse(
            totalHouseholdIncome = BigDecimal.TEN,
            totalHouseholdIncomeRecurring = BigDecimal.TEN,
            totalHouseholdExpenditure = BigDecimal.ONE,
            totalHouseholdDueExpenses = BigDecimal.ZERO,
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
            sharedHouseholdDueExpensesTotal = BigDecimal.ZERO,
            personalHouseholdDueExpensesTotal = BigDecimal.ZERO,
            sharedHouseholdReserveShare = BigDecimal.ZERO,
            budgetPerPerson = BigDecimal.ZERO,
            costSplit = emptyList()
        )


        `when`(budgetItemRepository.findEffectiveForMonth(BudgetItemType.INCOME, monthStart, monthEnd)).thenReturn(incomeItems)
        `when`(budgetItemRepository.findEffectiveForMonth(BudgetItemType.EXPENSE, monthStart, monthEnd)).thenReturn(expenseItems)
        `when`(budgetItemSuspensionRepository.findActiveForItemsAndMonth(listOf(1L), monthStart, monthEnd))
            .thenReturn(emptyList())
        `when`(budgetItemSuspensionRepository.findActiveForItemsAndMonth(listOf(2L), monthStart, monthEnd))
            .thenReturn(emptyList())
        `when`(personRepository.findAll()).thenReturn(persons)
        `when`(monthlySummaryCalculator.calculate(incomeItems, expenseItems, month, persons))
            .thenReturn(expectedResponse)

        // when
        val result = budgetService.monthlySummary(month)

        // then
        assertEquals(expectedResponse, result)
    }

    @Test
    fun `yearlyExpenseSummary should return monthly totals for shared expenses`() {
        val year = 2025
        val janStart = LocalDate.of(2025, 1, 1)
        val janEnd = LocalDate.of(2025, 1, 31)
        val expenseItem =
            BudgetItem(
                id = 1,
                name = "Ruecklage",
                amount = BigDecimal("1200.00"),
                type = BudgetItemType.EXPENSE,
                frequency = Frequency.YEARLY,
                planned = true,
                categoryCorrection = false,
                startDate = janStart,
                endDate = null,
                category = Category(1, "cat", BudgetItemType.EXPENSE, 1),
                person = null,
            )

        doAnswer { invocation ->
            val monthStart = invocation.getArgument<LocalDate>(2)
            if (monthStart.monthValue == 1) {
                listOf(expenseItem)
            } else {
                emptyList()
            }
        }.`when`(budgetItemRepository).findEffectiveForMonthByPerson(
            ArgumentMatchers.eq(BudgetItemType.EXPENSE) ?: BudgetItemType.EXPENSE,
            ArgumentMatchers.isNull(),
            ArgumentMatchers.any(LocalDate::class.java) ?: LocalDate.now(),
            ArgumentMatchers.any(LocalDate::class.java) ?: LocalDate.now(),
        )
        `when`(
            budgetItemSuspensionRepository.findActiveForItemsAndMonth(
                ArgumentMatchers.anyList<Long>() ?: emptyList(),
                ArgumentMatchers.any(LocalDate::class.java) ?: LocalDate.now(),
                ArgumentMatchers.any(LocalDate::class.java) ?: LocalDate.now(),
            ),
        ).thenReturn(emptyList())

        val result = budgetService.yearlyExpenseSummary(year, null)

        assertEquals(year, result.year)
        val january = result.months.first { it.month == "2025-01" }
        assertEquals(BigDecimal("100.00"), january.totalExpense)
        val february = result.months.first { it.month == "2025-02" }
        assertEquals(BigDecimal.ZERO, february.totalExpense)
    }
}
