/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.PersonCostSplitResponse
import com.fairshare.model.Person
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class CostSplitCalculator {
    fun calculate(
        persons: List<Person>,
        personalIncomeTotals: Map<Long?, BigDecimal>,
        personalExpenseTotals: Map<Long?, BigDecimal>,
        sharedIncomeTotal: BigDecimal,
        sharedExpenseTotal: BigDecimal,
        netResultShared: BigDecimal,
    ): CostSplitResult {
        val personalUsableIncomes =
            persons.associate { person ->
                val income = personalIncomeTotals[person.id] ?: BigDecimal.ZERO
                val expenses = personalExpenseTotals[person.id] ?: BigDecimal.ZERO
                person.id to income.subtract(expenses)
            }
        val budgetPerPerson =
            if (persons.isEmpty()) {
                BigDecimal.ZERO
            } else {
                netResultShared.divide(BigDecimal(persons.size), 2, RoundingMode.HALF_UP)
            }
        val costSplit =
            persons.map { person ->
                val income = personalIncomeTotals[person.id] ?: BigDecimal.ZERO
                val expenses = personalExpenseTotals[person.id] ?: BigDecimal.ZERO
                val personalUsableIncome = personalUsableIncomes[person.id] ?: BigDecimal.ZERO
                val personalCostShare = income.subtract(budgetPerPerson)
                val personalContribution =
                    if (personalUsableIncome > personalCostShare) {
                        personalCostShare
                    } else {
                        personalUsableIncome.max(BigDecimal.ZERO)
                    }
                PersonCostSplitResponse(
                    personId = person.id,
                    name = person.name,
                    personalIncome = income,
                    personalExpenses = expenses,
                    personalUsableIncome = personalUsableIncome,
                    personalCostShare = personalCostShare,
                    personalContribution = personalContribution,
                )
            }
        return CostSplitResult(
            budgetPerPerson = budgetPerPerson,
            costSplit = costSplit,
            sharedIncomeTotal = sharedIncomeTotal,
            sharedExpenseTotal = sharedExpenseTotal,
        )
    }
}

data class CostSplitResult(
    val budgetPerPerson: BigDecimal,
    val costSplit: List<PersonCostSplitResponse>,
    val sharedIncomeTotal: BigDecimal,
    val sharedExpenseTotal: BigDecimal,
)
