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
        val accountB = SavingsAccount(id = 2, name = "B")
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
        `when`(budgetService.monthlyTotals(from, to)).thenReturn(householdBalances)

        val result = savingsAccountBalanceService.monthlySummary(from, to)

        assertEquals(3, result.size)
        assertEquals(BigDecimal("100.00"), result[0].totalBalance)
        assertEquals(BigDecimal("80.00"), result[0].expectedBalance)
        assertEquals(BigDecimal("300.00"), result[1].totalBalance)
        assertEquals(BigDecimal("90.00"), result[1].expectedBalance)
        assertEquals(BigDecimal("350.00"), result[2].totalBalance)
        assertEquals(BigDecimal("85.00"), result[2].expectedBalance)
    }
}
