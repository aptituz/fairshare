/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.DataExportPayload
import com.fairshare.exception.BadRequestException
import com.fairshare.mapper.toExport
import com.fairshare.model.BudgetItem
import com.fairshare.model.BudgetItemSuspension
import com.fairshare.model.Category
import com.fairshare.model.Person
import com.fairshare.model.SavingsAccount
import com.fairshare.model.SavingsAccountBalance
import com.fairshare.repo.BudgetItemRepository
import com.fairshare.repo.BudgetItemSuspensionRepository
import com.fairshare.repo.CategoryRepository
import com.fairshare.repo.PersonRepository
import com.fairshare.repo.SavingsAccountBalanceRepository
import com.fairshare.repo.SavingsAccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DataTransferService(
    private val personRepository: PersonRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetItemRepository: BudgetItemRepository,
    private val budgetItemSuspensionRepository: BudgetItemSuspensionRepository,
    private val savingsAccountRepository: SavingsAccountRepository,
    private val savingsAccountBalanceRepository: SavingsAccountBalanceRepository,
) {
    fun exportData(): DataExportPayload =
        DataExportPayload(
            persons = personRepository.findAll().sortedBy { it.id }.map { it.toExport() },
            categories = categoryRepository.findAll().sortedBy { it.id }.map { it.toExport() },
            budgetItems = budgetItemRepository.findAll().sortedBy { it.id }.map { it.toExport() },
            budgetItemSuspensions = budgetItemSuspensionRepository.findAll().sortedBy { it.id }.map { it.toExport() },
            savingsAccounts = savingsAccountRepository.findAll().sortedBy { it.id }.map { it.toExport() },
            savingsAccountBalances = savingsAccountBalanceRepository.findAll().sortedBy { it.id }.map { it.toExport() },
        )

    @Transactional
    fun importData(payload: DataExportPayload) {
        validatePayload(payload)

        savingsAccountBalanceRepository.deleteAllInBatch()
        budgetItemSuspensionRepository.deleteAllInBatch()
        budgetItemRepository.deleteAllInBatch()
        savingsAccountRepository.deleteAllInBatch()
        categoryRepository.deleteAllInBatch()
        personRepository.deleteAllInBatch()

        val personsById =
            payload.persons.associate { export ->
                export.id to
                    personRepository.save(
                        Person(
                            name = export.name,
                            username = export.username,
                            passwordHash = export.passwordHash,
                            passwordSalt = export.passwordSalt,
                        ),
                    )
            }

        val categoriesById =
            payload.categories.associate { export ->
                export.id to
                    categoryRepository.save(
                        Category(
                            name = export.name,
                            type = export.type,
                            rank = export.rank,
                        ),
                    )
            }

        val budgetItemsById =
            payload.budgetItems.associate { export ->
                val category = export.categoryId?.let { categoriesById[it] }
                val person = export.personId?.let { personsById[it] }
                export.id to
                    budgetItemRepository.save(
                        BudgetItem(
                            name = export.name,
                            amount = export.amount,
                            type = export.type,
                            frequency = export.frequency,
                            planned = export.planned,
                            categoryCorrection = export.categoryCorrection,
                            startDate = export.startDate,
                            endDate = export.endDate,
                            dueDate = export.dueDate,
                            category = category,
                            person = person,
                        ),
                    )
            }

        payload.budgetItems.forEach { export ->
            val item = budgetItemsById.getValue(export.id)
            val previous = export.previousBudgetItemId?.let { budgetItemsById.getValue(it) }
            val root = export.rootBudgetItemId?.let { budgetItemsById.getValue(it) }
            if (item.previousBudgetItem != previous || item.rootBudgetItem != root) {
                item.previousBudgetItem = previous
                item.rootBudgetItem = root
                budgetItemRepository.save(item)
            }
        }

        payload.budgetItemSuspensions.forEach { export ->
            val budgetItem = budgetItemsById.getValue(export.budgetItemId)
            budgetItemSuspensionRepository.save(
                BudgetItemSuspension(
                    budgetItem = budgetItem,
                    startDate = export.startDate,
                    endDate = export.endDate,
                ),
            )
        }

        val accountsById =
            payload.savingsAccounts.associate { export ->
                val owner = export.ownerPersonId?.let { personsById[it] }
                export.id to
                    savingsAccountRepository.save(
                        SavingsAccount(
                            name = export.name,
                            owner = owner,
                            startDate = export.startDate,
                            endDate = export.endDate,
                        ),
                    )
            }

        payload.savingsAccountBalances.forEach { export ->
            val account = accountsById.getValue(export.savingsAccountId)
            savingsAccountBalanceRepository.save(
                SavingsAccountBalance(
                    savingsAccount = account,
                    balanceDate = export.balanceDate,
                    balanceAmount = export.balanceAmount,
                ),
            )
        }
    }

    private fun validatePayload(payload: DataExportPayload) {
        validateUniqueIds(payload.persons.map { it.id }, "persons")
        validateUniqueIds(payload.categories.map { it.id }, "categories")
        validateUniqueIds(payload.budgetItems.map { it.id }, "budgetItems")
        validateUniqueIds(payload.budgetItemSuspensions.map { it.id }, "budgetItemSuspensions")
        validateUniqueIds(payload.savingsAccounts.map { it.id }, "savingsAccounts")
        validateUniqueIds(payload.savingsAccountBalances.map { it.id }, "savingsAccountBalances")

        val personIds = payload.persons.map { it.id }.toSet()
        val categoryIds = payload.categories.map { it.id }.toSet()
        val budgetItemIds = payload.budgetItems.map { it.id }.toSet()
        val savingsAccountIds = payload.savingsAccounts.map { it.id }.toSet()

        payload.budgetItems.forEach { item ->
            if (item.categoryId != null && item.categoryId !in categoryIds) {
                throw BadRequestException("Unknown category id ${item.categoryId} in budget items.")
            }
            if (item.personId != null && item.personId !in personIds) {
                throw BadRequestException("Unknown person id ${item.personId} in budget items.")
            }
            if (item.previousBudgetItemId != null && item.previousBudgetItemId !in budgetItemIds) {
                throw BadRequestException("Unknown previous budget item id ${item.previousBudgetItemId}.")
            }
            if (item.rootBudgetItemId != null && item.rootBudgetItemId !in budgetItemIds) {
                throw BadRequestException("Unknown root budget item id ${item.rootBudgetItemId}.")
            }
        }

        payload.budgetItemSuspensions.forEach { suspension ->
            if (suspension.budgetItemId !in budgetItemIds) {
                throw BadRequestException("Unknown budget item id ${suspension.budgetItemId} in suspensions.")
            }
        }

        payload.savingsAccounts.forEach { account ->
            if (account.ownerPersonId != null && account.ownerPersonId !in personIds) {
                throw BadRequestException("Unknown person id ${account.ownerPersonId} in savings accounts.")
            }
        }

        payload.savingsAccountBalances.forEach { balance ->
            if (balance.savingsAccountId !in savingsAccountIds) {
                throw BadRequestException("Unknown savings account id ${balance.savingsAccountId} in balances.")
            }
        }
    }

    private fun validateUniqueIds(
        ids: List<Long>,
        label: String,
    ) {
        val duplicates =
            ids
                .groupingBy { it }
                .eachCount()
                .filterValues { it > 1 }
                .keys
        if (duplicates.isNotEmpty()) {
            throw BadRequestException("Duplicate ids in $label: ${duplicates.sorted().joinToString(", ")}.")
        }
    }
}
