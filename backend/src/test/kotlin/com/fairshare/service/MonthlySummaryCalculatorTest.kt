/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.model.BudgetItem
import com.fairshare.model.BudgetItemType
import com.fairshare.model.Frequency
import com.fairshare.model.Person
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class MonthlySummaryCalculatorTest {
    private val calculator = MonthlySummaryCalculator(CostSplitCalculator())

    @Test
    fun `case A uses personal cost share when usable income exceeds cost share`() {
        val personA = Person(id = 1, name = "Alex")
        val personB = Person(id = 2, name = "Bo")

        val incomeItems =
            listOf(
                budgetItem("Income A", "3000.00", BudgetItemType.INCOME, personA),
                budgetItem("Income B", "2000.00", BudgetItemType.INCOME, personB),
            )
        val expenseItems =
            listOf(
                budgetItem("Personal A", "500.00", BudgetItemType.EXPENSE, personA),
                budgetItem("Personal B", "500.00", BudgetItemType.EXPENSE, personB),
                budgetItem("Shared", "1000.00", BudgetItemType.EXPENSE, null),
            )

        val summary = calculator.calculate(incomeItems, expenseItems, listOf(personA, personB))

        assertBigDecimalEquals("2000.00", summary.budgetPerPerson)
        assertBigDecimalEquals("4000.00", summary.sharedHouseholdBudgetBalanceWithoutOneTimeIncome)
        assertBigDecimalEquals("5000.00", summary.totalHouseholdIncome)
        assertBigDecimalEquals("2000.00", summary.totalHouseholdExpenditure)
        assertBigDecimalEquals("3000.00", summary.householdBudgetBalance)

        val aSplit = summary.costSplit.first { it.personId == personA.id }
        val bSplit = summary.costSplit.first { it.personId == personB.id }

        assertBigDecimalEquals("1000.00", aSplit.personalCostShare)
        assertBigDecimalEquals("0.00", bSplit.personalCostShare)
        assertBigDecimalEquals("1000.00", aSplit.personalContribution)
        assertBigDecimalEquals("0.00", bSplit.personalContribution)
    }

    @Test
    fun `case B contributes usable income when cost share exceeds usable income`() {
        val personA = Person(id = 1, name = "Alex")
        val personB = Person(id = 2, name = "Bo")

        val incomeItems =
            listOf(
                budgetItem("Income A", "2000.00", BudgetItemType.INCOME, personA),
                budgetItem("Income B", "1500.00", BudgetItemType.INCOME, personB),
            )
        val expenseItems =
            listOf(
                budgetItem("Personal A", "800.00", BudgetItemType.EXPENSE, personA),
                budgetItem("Personal B", "1000.00", BudgetItemType.EXPENSE, personB),
                budgetItem("Shared", "2900.00", BudgetItemType.EXPENSE, null),
            )

        val summary = calculator.calculate(incomeItems, expenseItems, listOf(personA, personB))

        assertBigDecimalEquals("300.00", summary.budgetPerPerson)
        assertBigDecimalEquals("600.00", summary.sharedHouseholdBudgetBalanceWithoutOneTimeIncome)
        assertBigDecimalEquals("3500.00", summary.totalHouseholdIncome)
        assertBigDecimalEquals("4700.00", summary.totalHouseholdExpenditure)
        assertBigDecimalEquals("-1200.00", summary.householdBudgetBalance)

        val aSplit = summary.costSplit.first { it.personId == personA.id }
        val bSplit = summary.costSplit.first { it.personId == personB.id }

        assertBigDecimalEquals("1700.00", aSplit.personalCostShare)
        assertBigDecimalEquals("1200.00", bSplit.personalCostShare)
        assertBigDecimalEquals("1200.00", aSplit.personalContribution)
        assertBigDecimalEquals("500.00", bSplit.personalContribution)
    }

    private fun budgetItem(
        name: String,
        amount: String,
        type: BudgetItemType,
        person: Person?,
    ): BudgetItem =
        BudgetItem(
            name = name,
            amount = BigDecimal(amount),
            type = type,
            frequency = Frequency.MONTHLY,
            active = true,
            planned = true,
            categoryCorrection = false,
            startDate = LocalDate.of(2025, 3, 1),
            endDate = null,
            category = null,
            person = person,
        )

    private fun assertBigDecimalEquals(
        expected: String,
        actual: BigDecimal,
    ) {
        assertEquals(0, BigDecimal(expected).compareTo(actual), "Expected $expected but was $actual")
    }
}
