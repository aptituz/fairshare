/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.MonthlyTotalsResponse
import com.fairshare.model.SavingsAccount
import com.fairshare.model.SavingsAccountBalance
import com.fairshare.repo.SavingsAccountBalanceRepository
import com.fairshare.repo.SavingsAccountRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@ExtendWith(MockitoExtension::class)
class SavingsAccountBalanceServiceTest {

    @Mock
    lateinit var savingsAccountRepository: SavingsAccountRepository

    @Mock
    lateinit var savingsAccountBalanceRepository: SavingsAccountBalanceRepository

    @Mock
    lateinit var budgetService: BudgetService

    @InjectMocks
    lateinit var savingsAccountBalanceService: SavingsAccountBalanceService

    @Test
    fun `monthlySummary should include expected balance based on household budget balances`() {
        val accountA = SavingsAccount(id = 1, name = "A")
        val accountB = SavingsAccount(
            id = 2,
            name = "B",
            startDate = LocalDate.of(2025, 2, 1),
            endDate = LocalDate.of(2025, 2, 28)
        )
        val from = YearMonth.of(2025, 1)
        val to = YearMonth.of(2025, 3)

        val balances =
            listOf(
                SavingsAccountBalance(
                    id = 4,
                    savingsAccount = accountA,
                    balanceDate = LocalDate.of(2024, 12, 31),
                    balanceAmount = BigDecimal("80.00"),
                ),
                SavingsAccountBalance(
                    id = 1,
                    savingsAccount = accountA,
                    balanceDate = LocalDate.of(2025, 1, 15),
                    balanceAmount = BigDecimal("100.00"),
                ),
                SavingsAccountBalance(
                    id = 2,
                    savingsAccount = accountB,
                    balanceDate = LocalDate.of(2025, 2, 10),
                    balanceAmount = BigDecimal("200.00"),
                ),
                SavingsAccountBalance(
                    id = 3,
                    savingsAccount = accountA,
                    balanceDate = LocalDate.of(2025, 3, 5),
                    balanceAmount = BigDecimal("150.00"),
                ),
            )

        val householdBalances =
            listOf(
                MonthlyTotalsResponse(
                    month = "2025-01",
                    totalHouseholdIncome = BigDecimal("10.00"),
                    totalHouseholdExpenditure = BigDecimal("0.00"),
                    householdBudgetBalance = BigDecimal("10.00"),
                ),
                MonthlyTotalsResponse(
                    month = "2025-02",
                    totalHouseholdIncome = BigDecimal("0.00"),
                    totalHouseholdExpenditure = BigDecimal("5.00"),
                    householdBudgetBalance = BigDecimal("-5.00"),
                ),
                MonthlyTotalsResponse(
                    month = "2025-03",
                    totalHouseholdIncome = BigDecimal("20.00"),
                    totalHouseholdExpenditure = BigDecimal("0.00"),
                    householdBudgetBalance = BigDecimal("20.00"),
                ),
            )

        `when`(savingsAccountRepository.findAll()).thenReturn(listOf(accountA, accountB))
        `when`(
            savingsAccountBalanceRepository.findBySavingsAccountIdsUpToDate(
                listOf(1L, 2L),
                LocalDate.of(2025, 3, 31),
            ),
        ).thenReturn(balances)
        `when`(savingsAccountRepository.findActiveIdsForMonth(any(), any()))
            .thenReturn(listOf(1L, 2L))
        `when`(budgetService.monthlyTotals(from, to)).thenReturn(householdBalances)

        val result = savingsAccountBalanceService.monthlySummary(from, to)

        assertEquals(3, result.size)
        assertEquals(BigDecimal("100.00"), result[0].totalBalance)
        assertEquals(BigDecimal("80.00"), result[0].expectedBalance)
        assertEquals(BigDecimal("10.00"), result[0].expectedMonthlySavings)
        assertEquals(BigDecimal("300.00"), result[1].totalBalance)
        assertEquals(BigDecimal("90.00"), result[1].expectedBalance)
        assertEquals(BigDecimal("-5.00"), result[1].expectedMonthlySavings)
        assertEquals(BigDecimal("150.00"), result[2].totalBalance)
        assertEquals(BigDecimal("85.00"), result[2].expectedBalance)
        assertEquals(BigDecimal("20.00"), result[2].expectedMonthlySavings)
    }

    @Test
    fun `monthlyBalances should return summary totals with balances grouped by month`() {
        val accountA = SavingsAccount(id = 1, name = "A")
        val from = YearMonth.of(2025, 1)
        val to = YearMonth.of(2025, 12)

        val balances =
            listOf(
                SavingsAccountBalance(
                    id = 4,
                    savingsAccount = accountA,
                    balanceDate = LocalDate.of(2024, 12, 31),
                    balanceAmount = BigDecimal("100.00"),
                ),
                SavingsAccountBalance(
                    id = 1,
                    savingsAccount = accountA,
                    balanceDate = LocalDate.of(2025, 1, 10),
                    balanceAmount = BigDecimal("200.00"),
                ),
                SavingsAccountBalance(
                    id = 2,
                    savingsAccount = accountA,
                    balanceDate = LocalDate.of(2025, 2, 15),
                    balanceAmount = BigDecimal("300.00"),
                ),
            )

        val householdBalances =
            listOf(
                MonthlyTotalsResponse(
                    month = "2025-01",
                    totalHouseholdIncome = BigDecimal("10.00"),
                    totalHouseholdExpenditure = BigDecimal("0.00"),
                    householdBudgetBalance = BigDecimal("10.00"),
                ),
            )

        `when`(savingsAccountRepository.findAll()).thenReturn(listOf(accountA))
        `when`(
            savingsAccountBalanceRepository.findBySavingsAccountIdsUpToDate(
                listOf(1L),
                LocalDate.of(2025, 12, 31),
            ),
        ).thenReturn(balances)
        `when`(savingsAccountRepository.findActiveIdsForMonth(any(), any()))
            .thenReturn(listOf(1L))
        `when`(budgetService.monthlyTotals(from, to)).thenReturn(householdBalances)
        `when`(
            savingsAccountBalanceRepository.findByBalanceDateBetweenOrderByBalanceDateDescIdDesc(
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
            ),
        ).thenReturn(
            balances.filter { it.balanceDate.year == 2025 }
                .sortedByDescending { it.balanceDate },
        )

        val result = savingsAccountBalanceService.monthlyBalances(2025)

        assertEquals(12, result.size)
        val january = result.first { it.month == "2025-01" }
        assertEquals(BigDecimal("200.00"), january.totalBalance)
        assertEquals(1, january.balances.size)
        assertEquals(BigDecimal("200.00"), january.balances[0].balanceAmount)
        val march = result.first { it.month == "2025-03" }
        assertTrue(march.balances.isEmpty())
    }
}
