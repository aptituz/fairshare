/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.model.Person
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CostSplitCalculatorTest {

    private val costSplitCalculator = CostSplitCalculator()

    @Test
    fun `calculate should return correct split for a typical scenario`() {
        // ARRANGE
        val person1 = Person(id = 1L, name = "Alice")
        val person2 = Person(id = 2L, name = "Bob")
        val persons = listOf(person1, person2)

        val personalIncomeTotals = mapOf<Long?, BigDecimal>(
            1L to BigDecimal("2000.00"), // Alice's income
            2L to BigDecimal("3000.00")  // Bob's income
        )
        val personalExpenseTotals = mapOf<Long?, BigDecimal>(
            1L to BigDecimal("500.00"), // Alice's expenses
            2L to BigDecimal("800.00")  // Bob's expenses
        )
        val sharedIncomeTotal = BigDecimal("1000.00")
        val sharedExpenseTotal = BigDecimal("1200.00")
        // Net result of shared items is 1000 - 1200 = -200
        val netResultShared = sharedIncomeTotal.subtract(sharedExpenseTotal)

        // ACT
        val result = costSplitCalculator.calculate(
            persons = persons,
            personalIncomeTotals = personalIncomeTotals,
            personalExpenseTotals = personalExpenseTotals,
            sharedIncomeTotal = sharedIncomeTotal,
            sharedExpenseTotal = sharedExpenseTotal,
            netResultShared = netResultShared
        )

        // ASSERT
        // 1. Overall Budget & Shared Totals
        // The shared deficit is -200. Split between 2 people, so each should cover -100.
        assertEquals(0, result.budgetPerPerson.compareTo(BigDecimal("-100.00")))
        assertEquals(0, result.sharedIncomeTotal.compareTo(sharedIncomeTotal))
        assertEquals(0, result.sharedExpenseTotal.compareTo(sharedExpenseTotal))


        // 2. Individual Splits (Alice and Bob)
        assertEquals(2, result.costSplit.size)
        val aliceSplit = result.costSplit.find { it.personId == 1L }
        val bobSplit = result.costSplit.find { it.personId == 2L }
        assertNotNull(aliceSplit, "Alice's split should not be null")
        assertNotNull(bobSplit, "Bob's split should not be null")

        // 3. Verify Alice's Split
        assertEquals("Alice", aliceSplit.name)
        assertEquals(0, aliceSplit.personalIncome.compareTo(BigDecimal("2000.00")))
        assertEquals(0, aliceSplit.personalExpenses.compareTo(BigDecimal("500.00")))
        // Usable income: 2000 - 500 = 1500
        assertEquals(0, aliceSplit.personalUsableIncome.compareTo(BigDecimal("1500.00")))
        // Her cost share: personal income (2000) - budget per person (-100) = 2100
        assertEquals(0, aliceSplit.personalCostShare.compareTo(BigDecimal("2100.00")))
        // Her contribution is the lesser of her usable income (1500) and her cost share (2100)
        assertEquals(0, aliceSplit.personalContribution.compareTo(BigDecimal("1500.00")))

        // 4. Verify Bob's Split
        assertEquals("Bob", bobSplit!!.name)
        assertEquals(0, bobSplit.personalIncome.compareTo(BigDecimal("3000.00")))
        assertEquals(0, bobSplit.personalExpenses.compareTo(BigDecimal("800.00")))
        // Usable income: 3000 - 800 = 2200
        assertEquals(0, bobSplit.personalUsableIncome.compareTo(BigDecimal("2200.00")))
        // His cost share: personal income (3000) - budget per person (-100) = 3100
        assertEquals(0, bobSplit.personalCostShare.compareTo(BigDecimal("3100.00")))
        // His contribution is the lesser of his usable income (2200) and his cost share (3100)
        assertEquals(0, bobSplit.personalContribution.compareTo(BigDecimal("2200.00")))
    }
}
