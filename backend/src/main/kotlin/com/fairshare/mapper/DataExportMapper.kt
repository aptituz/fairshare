/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.mapper

import com.fairshare.dto.BudgetItemExport
import com.fairshare.dto.BudgetItemSuspensionExport
import com.fairshare.dto.CategoryExport
import com.fairshare.dto.PersonExport
import com.fairshare.dto.SavingsAccountBalanceExport
import com.fairshare.dto.SavingsAccountExport
import com.fairshare.model.BudgetItem
import com.fairshare.model.BudgetItemSuspension
import com.fairshare.model.Category
import com.fairshare.model.Person
import com.fairshare.model.SavingsAccount
import com.fairshare.model.SavingsAccountBalance

fun Person.toExport(): PersonExport =
    PersonExport(
        id = requireNotNull(id),
        name = name,
        username = username,
        passwordHash = passwordHash,
        passwordSalt = passwordSalt,
    )

fun Category.toExport(): CategoryExport =
    CategoryExport(
        id = requireNotNull(id),
        name = name,
        type = type,
        rank = rank,
    )

fun BudgetItem.toExport(): BudgetItemExport =
    BudgetItemExport(
        id = requireNotNull(id),
        name = name,
        amount = amount,
        type = type,
        frequency = frequency,
        planned = planned,
        categoryCorrection = categoryCorrection,
        startDate = startDate,
        endDate = endDate,
        categoryId = category?.id,
        personId = person?.id,
        previousBudgetItemId = previousBudgetItem?.id,
        rootBudgetItemId = rootBudgetItem?.id,
    )

fun BudgetItemSuspension.toExport(): BudgetItemSuspensionExport =
    BudgetItemSuspensionExport(
        id = requireNotNull(id),
        budgetItemId = requireNotNull(budgetItem.id),
        startDate = startDate,
        endDate = endDate,
    )

fun SavingsAccount.toExport(): SavingsAccountExport =
    SavingsAccountExport(
        id = requireNotNull(id),
        name = name,
        ownerPersonId = owner?.id,
        startDate = startDate,
        endDate = endDate,
    )

fun SavingsAccountBalance.toExport(): SavingsAccountBalanceExport =
    SavingsAccountBalanceExport(
        id = requireNotNull(id),
        savingsAccountId = requireNotNull(savingsAccount.id),
        balanceDate = balanceDate,
        balanceAmount = balanceAmount,
    )
