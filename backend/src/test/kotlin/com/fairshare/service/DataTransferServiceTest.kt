/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.BudgetItemExport
import com.fairshare.dto.BudgetItemSuspensionExport
import com.fairshare.dto.CategoryExport
import com.fairshare.dto.DataExportPayload
import com.fairshare.dto.PersonExport
import com.fairshare.dto.SavingsAccountBalanceExport
import com.fairshare.dto.SavingsAccountExport
import com.fairshare.exception.BadRequestException
import com.fairshare.model.BudgetItem
import com.fairshare.model.BudgetItemSuspension
import com.fairshare.model.BudgetItemType
import com.fairshare.model.Category
import com.fairshare.model.Frequency
import com.fairshare.model.Person
import com.fairshare.model.SavingsAccount
import com.fairshare.model.SavingsAccountBalance
import com.fairshare.repo.BudgetItemRepository
import com.fairshare.repo.BudgetItemSuspensionRepository
import com.fairshare.repo.CategoryRepository
import com.fairshare.repo.PersonRepository
import com.fairshare.repo.SavingsAccountBalanceRepository
import com.fairshare.repo.SavingsAccountRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class DataTransferServiceTest {
    @Mock
    lateinit var personRepository: PersonRepository

    @Mock
    lateinit var categoryRepository: CategoryRepository

    @Mock
    lateinit var budgetItemRepository: BudgetItemRepository

    @Mock
    lateinit var budgetItemSuspensionRepository: BudgetItemSuspensionRepository

    @Mock
    lateinit var savingsAccountRepository: SavingsAccountRepository

    @Mock
    lateinit var savingsAccountBalanceRepository: SavingsAccountBalanceRepository

    @InjectMocks
    lateinit var dataTransferService: DataTransferService

    @Test
    fun `export should return mapped data`() {
        val person = Person(2, "Alex", "alex", "hash", "salt")
        val category = Category(1, "Gehalt", BudgetItemType.INCOME, 5)
        val budgetItem =
            BudgetItem(
                id = 3,
                name = "Miete",
                amount = BigDecimal("1200"),
                type = BudgetItemType.EXPENSE,
                frequency = Frequency.MONTHLY,
                planned = true,
                categoryCorrection = false,
                startDate = LocalDate.of(2025, 1, 1),
                endDate = null,
                category = category,
                person = person,
            )
        val suspension =
            BudgetItemSuspension(
                id = 4,
                budgetItem = budgetItem,
                startDate = LocalDate.of(2025, 2, 1),
                endDate = null,
            )
        val account =
            SavingsAccount(
                id = 5,
                name = "Ruecklage",
                owner = person,
                startDate = LocalDate.of(2025, 1, 1),
                endDate = null,
            )
        val balance =
            SavingsAccountBalance(
                id = 6,
                savingsAccount = account,
                balanceDate = LocalDate.of(2025, 1, 1),
                balanceAmount = BigDecimal("500"),
            )

        `when`(personRepository.findAll()).thenReturn(listOf(person))
        `when`(categoryRepository.findAll()).thenReturn(listOf(category))
        `when`(budgetItemRepository.findAll()).thenReturn(listOf(budgetItem))
        `when`(budgetItemSuspensionRepository.findAll()).thenReturn(listOf(suspension))
        `when`(savingsAccountRepository.findAll()).thenReturn(listOf(account))
        `when`(savingsAccountBalanceRepository.findAll()).thenReturn(listOf(balance))

        val payload = dataTransferService.exportData()

        assertEquals(1, payload.persons.size)
        assertEquals("Alex", payload.persons[0].name)
        assertEquals(1, payload.categories.size)
        assertEquals("Gehalt", payload.categories[0].name)
        assertEquals(1, payload.budgetItems.size)
        assertEquals("Miete", payload.budgetItems[0].name)
        assertEquals(1, payload.budgetItemSuspensions.size)
        assertEquals(1, payload.savingsAccounts.size)
        assertEquals(1, payload.savingsAccountBalances.size)
    }

    @Test
    fun `import should overwrite data and connect relations`() {
        val payload =
            DataExportPayload(
                persons =
                    listOf(
                        PersonExport(id = 10, name = "Alex", username = "alex"),
                    ),
                categories =
                    listOf(
                        CategoryExport(id = 20, name = "Gehalt", type = BudgetItemType.INCOME, rank = 1),
                    ),
                budgetItems =
                    listOf(
                        BudgetItemExport(
                            id = 30,
                            name = "Miete",
                            amount = BigDecimal("800"),
                            type = BudgetItemType.EXPENSE,
                            frequency = Frequency.MONTHLY,
                            planned = true,
                            categoryCorrection = false,
                            startDate = LocalDate.of(2025, 1, 1),
                            endDate = null,
                            dueDate = null,
                            categoryId = 20,
                            personId = 10,
                            previousBudgetItemId = null,
                            rootBudgetItemId = 30,
                        ),
                        BudgetItemExport(
                            id = 31,
                            name = "Miete neu",
                            amount = BigDecimal("900"),
                            type = BudgetItemType.EXPENSE,
                            frequency = Frequency.MONTHLY,
                            planned = true,
                            categoryCorrection = false,
                            startDate = LocalDate.of(2025, 2, 1),
                            endDate = null,
                            dueDate = null,
                            categoryId = 20,
                            personId = 10,
                            previousBudgetItemId = 30,
                            rootBudgetItemId = 30,
                        ),
                    ),
                budgetItemSuspensions =
                    listOf(
                        BudgetItemSuspensionExport(
                            id = 40,
                            budgetItemId = 31,
                            startDate = LocalDate.of(2025, 3, 1),
                            endDate = null,
                        ),
                    ),
                savingsAccounts =
                    listOf(
                        SavingsAccountExport(
                            id = 50,
                            name = "Ruecklage",
                            ownerPersonId = 10,
                            startDate = LocalDate.of(2025, 1, 1),
                            endDate = null,
                        ),
                    ),
                savingsAccountBalances =
                    listOf(
                        SavingsAccountBalanceExport(
                            id = 60,
                            savingsAccountId = 50,
                            balanceDate = LocalDate.of(2025, 1, 5),
                            balanceAmount = BigDecimal("500"),
                        ),
                    ),
            )

        `when`(personRepository.save(any(Person::class.java))).thenAnswer { it.arguments[0] as Person }
        `when`(categoryRepository.save(any(Category::class.java))).thenAnswer { it.arguments[0] as Category }
        `when`(budgetItemRepository.save(any(BudgetItem::class.java))).thenAnswer { it.arguments[0] as BudgetItem }
        `when`(budgetItemSuspensionRepository.save(any(BudgetItemSuspension::class.java)))
            .thenAnswer { it.arguments[0] as BudgetItemSuspension }
        `when`(savingsAccountRepository.save(any(SavingsAccount::class.java)))
            .thenAnswer { it.arguments[0] as SavingsAccount }
        `when`(savingsAccountBalanceRepository.save(any(SavingsAccountBalance::class.java)))
            .thenAnswer { it.arguments[0] as SavingsAccountBalance }

        dataTransferService.importData(payload)

        verify(personRepository).deleteAllInBatch()
        verify(categoryRepository).deleteAllInBatch()
        verify(budgetItemRepository).deleteAllInBatch()
        verify(budgetItemSuspensionRepository).deleteAllInBatch()
        verify(savingsAccountRepository).deleteAllInBatch()
        verify(savingsAccountBalanceRepository).deleteAllInBatch()

        val budgetItemCaptor = ArgumentCaptor.forClass(BudgetItem::class.java)
        verify(budgetItemRepository, atLeastOnce()).save(budgetItemCaptor.capture())
        val linked = budgetItemCaptor.allValues.find { it.name == "Miete neu" }
        assertNotNull(linked?.previousBudgetItem)
        assertEquals("Miete", linked?.previousBudgetItem?.name)
        assertEquals("Miete", linked?.rootBudgetItem?.name)
    }

    @Test
    fun `import should reject invalid references`() {
        val payload =
            DataExportPayload(
                persons = listOf(PersonExport(id = 10, name = "Alex", username = "alex")),
                categories = emptyList(),
                budgetItems =
                    listOf(
                        BudgetItemExport(
                            id = 30,
                            name = "Miete",
                            amount = BigDecimal("800"),
                            type = BudgetItemType.EXPENSE,
                            frequency = Frequency.MONTHLY,
                            planned = true,
                            categoryCorrection = false,
                            startDate = LocalDate.of(2025, 1, 1),
                            endDate = null,
                            dueDate = null,
                            categoryId = 99,
                            personId = 10,
                            previousBudgetItemId = null,
                            rootBudgetItemId = null,
                        ),
                    ),
                budgetItemSuspensions = emptyList(),
                savingsAccounts = emptyList(),
                savingsAccountBalances = emptyList(),
            )

        assertThrows(BadRequestException::class.java) {
            dataTransferService.importData(payload)
        }

        verifyNoInteractions(
            personRepository,
            categoryRepository,
            budgetItemRepository,
            budgetItemSuspensionRepository,
            savingsAccountRepository,
            savingsAccountBalanceRepository,
        )
    }
}
