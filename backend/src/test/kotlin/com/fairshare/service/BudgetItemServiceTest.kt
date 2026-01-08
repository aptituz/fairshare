/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.CreateBudgetItemRequest
import com.fairshare.dto.UpdateBudgetItemRequest
import com.fairshare.model.BudgetItem
import com.fairshare.model.BudgetItemType
import com.fairshare.model.Category
import com.fairshare.model.Person
import com.fairshare.repo.BudgetItemRepository
import com.fairshare.repo.CategoryRepository
import com.fairshare.repo.PersonRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class BudgetItemServiceTest {

    @Mock
    lateinit var budgetItemRepository: BudgetItemRepository

    @Mock
    lateinit var categoryRepository: CategoryRepository

    @Mock
    lateinit var personRepository: PersonRepository

    @InjectMocks
    lateinit var budgetItemService: BudgetItemService

    @Test
    fun `list should return all budget items when no type is specified`() {
        // given
        val budgetItems = listOf(
            BudgetItem(id = 1, name = "Item 1", amount = BigDecimal.TEN, type = BudgetItemType.INCOME, category = Category(1, "cat", BudgetItemType.INCOME, 1), startDate = LocalDate.now()),
            BudgetItem(id = 2, name = "Item 2", amount = BigDecimal.ONE, type = BudgetItemType.EXPENSE, category = Category(2, "cat", BudgetItemType.EXPENSE, 2), startDate = LocalDate.now())
        )
        `when`(budgetItemRepository.findAll()).thenReturn(budgetItems)

        // when
        val result = budgetItemService.list(null)

        // then
        assertEquals(2, result.size)
    }

    @Test
    fun `list should return budget items of specified type`() {
        // given
        val budgetItems = listOf(
            BudgetItem(id = 1, name = "Item 1", amount = BigDecimal.TEN, type = BudgetItemType.INCOME, category = Category(1, "cat", BudgetItemType.INCOME, 1), startDate = LocalDate.now())
        )
        `when`(budgetItemRepository.findByType(BudgetItemType.INCOME)).thenReturn(budgetItems)

        // when
        val result = budgetItemService.list(BudgetItemType.INCOME)

        // then
        assertEquals(1, result.size)
        assertEquals(BudgetItemType.INCOME, result[0].type)
    }

    @Test
    fun `create should save and return a new budget item`() {
        // given
        val category = Category(1, "Category", BudgetItemType.INCOME, 1)
        val person = Person(1, "Person")
        val request = CreateBudgetItemRequest(
            name = "New Item",
            amount = BigDecimal.TEN,
            type = BudgetItemType.INCOME,
            categoryId = 1,
            personId = 1
        )
        `when`(categoryRepository.findById(1)).thenReturn(java.util.Optional.of(category))
        `when`(personRepository.findById(1)).thenReturn(java.util.Optional.of(person))
        `when`(budgetItemRepository.save(any(BudgetItem::class.java))).thenAnswer { it.arguments[0] as BudgetItem }

        // when
        val result = budgetItemService.create(request)

        // then
        assertEquals("New Item", result.name)
        assertEquals(BigDecimal.TEN, result.amount)
        assertEquals(BudgetItemType.INCOME, result.type)
        assertEquals(1, result.category?.id)
        assertEquals(1, result.person?.id)
    }

    @Test
    fun `create should throw an exception when category is not found`() {
        // given
        val request = CreateBudgetItemRequest(
            name = "New Item",
            amount = BigDecimal.TEN,
            type = BudgetItemType.INCOME,
            categoryId = 1
        )
        `when`(categoryRepository.findById(1)).thenReturn(java.util.Optional.empty())

        // when / then
        assertThrows(ResponseStatusException::class.java) {
            budgetItemService.create(request)
        }
    }

    @Test
    fun `create should throw an exception when person is not found`() {
        // given
        val request = CreateBudgetItemRequest(
            name = "New Item",
            amount = BigDecimal.TEN,
            type = BudgetItemType.INCOME,
            categoryId = null,
            personId = 1
        )
        `when`(personRepository.findById(1)).thenReturn(java.util.Optional.empty())

        // when / then
        assertThrows(ResponseStatusException::class.java) {
            budgetItemService.create(request)
        }
    }

    @Test
    fun `update should save and return an updated budget item`() {
        // given
        val budgetItem = BudgetItem(id = 1, name = "Old Item", amount = BigDecimal.ONE, type = BudgetItemType.INCOME, startDate = LocalDate.now())
        val category = Category(1, "Category", BudgetItemType.INCOME, 1)
        val person = Person(1, "Person")
        val request = UpdateBudgetItemRequest(
            name = "Updated Item",
            amount = BigDecimal.TEN,
            categoryId = 1,
            personId = 1
        )
        `when`(budgetItemRepository.findById(1)).thenReturn(java.util.Optional.of(budgetItem))
        `when`(categoryRepository.findById(1)).thenReturn(java.util.Optional.of(category))
        `when`(personRepository.findById(1)).thenReturn(java.util.Optional.of(person))
        `when`(budgetItemRepository.save(any(BudgetItem::class.java))).thenAnswer { it.arguments[0] as BudgetItem }

        // when
        val result = budgetItemService.update(1, request)

        // then
        assertEquals("Updated Item", result.name)
        assertEquals(BigDecimal.TEN, result.amount)
        assertEquals(1, result.category?.id)
        assertEquals(1, result.person?.id)
    }

    @Test
    fun `delete should remove a budget item`() {
        // given
        val budgetItem = BudgetItem(id = 1, name = "Item", amount = BigDecimal.TEN, type = BudgetItemType.INCOME, startDate = LocalDate.now())
        `when`(budgetItemRepository.findById(1)).thenReturn(java.util.Optional.of(budgetItem))

        // when
        budgetItemService.delete(1)

        // then
        // No exception should be thrown
    }
}
