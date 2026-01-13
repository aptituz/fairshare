/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.BudgetItemOverrideRequest
import com.fairshare.dto.BudgetItemValueChangeRequest
import com.fairshare.dto.CategoryCorrectionRequest
import com.fairshare.dto.CreateBudgetItemRequest
import com.fairshare.dto.ResumeBudgetItemRequest
import com.fairshare.dto.SuspendBudgetItemRequest
import com.fairshare.dto.UpdateBudgetItemRequest
import com.fairshare.exception.BadRequestException
import com.fairshare.exception.NotFoundException
import com.fairshare.model.BudgetItem
import com.fairshare.model.BudgetItemType
import com.fairshare.model.BudgetItemSuspension
import com.fairshare.model.Category
import com.fairshare.model.Frequency
import com.fairshare.model.Person
import com.fairshare.repo.BudgetItemRepository
import com.fairshare.repo.BudgetItemSuspensionRepository
import com.fairshare.repo.CategoryRepository
import com.fairshare.repo.PersonRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.ArgumentCaptor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class BudgetItemServiceTest {

    @Mock
    lateinit var budgetItemRepository: BudgetItemRepository

    @Mock
    lateinit var budgetItemSuspensionRepository: BudgetItemSuspensionRepository

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
        val result = budgetItemService.list(null, null)

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
        val result = budgetItemService.list(BudgetItemType.INCOME, null)

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
        assertThrows(NotFoundException::class.java) {
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
        assertThrows(NotFoundException::class.java) {
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

    @Test
    fun `overrideForMonth should throw an exception when override month is outside the item range`() {
        // given
        val budgetItem = BudgetItem(id = 1, name = "Item", amount = BigDecimal.TEN, type = BudgetItemType.INCOME, startDate = LocalDate.of(2025, 2, 1), endDate = LocalDate.of(2025, 3, 31))
        val request = BudgetItemOverrideRequest(month = "2025-01", amount = BigDecimal.ONE)
        `when`(budgetItemRepository.findById(1)).thenReturn(java.util.Optional.of(budgetItem))

        // when / then
        assertThrows(BadRequestException::class.java) {
            budgetItemService.overrideForMonth(1, request)
        }
    }

    @Test
    fun `overrideForMonth should update a one-time item successfully`() {
        // given
        val budgetItem = BudgetItem(id = 1, name = "Item", amount = BigDecimal.TEN, type = BudgetItemType.INCOME, frequency = com.fairshare.model.Frequency.ONE_TIME, startDate = LocalDate.of(2025, 1, 15))
        val request = BudgetItemOverrideRequest(month = "2025-01", amount = BigDecimal.ONE)
        `when`(budgetItemRepository.findById(1)).thenReturn(java.util.Optional.of(budgetItem))
        `when`(budgetItemRepository.save(any(BudgetItem::class.java))).thenAnswer { it.arguments[0] as BudgetItem }

        // when
        val result = budgetItemService.overrideForMonth(1, request)

        // then
        assertEquals(BigDecimal.ONE, result.amount)
    }

    @Test
    fun `overrideForMonth should split a recurring item`() {
        // given
        val budgetItem = BudgetItem(id = 1, name = "Item", amount = BigDecimal.TEN, type = BudgetItemType.INCOME, startDate = LocalDate.of(2024, 1, 1), endDate = LocalDate.of(2025, 12, 31))
        val request = BudgetItemOverrideRequest(month = "2025-06", amount = BigDecimal.ONE)
        `when`(budgetItemRepository.findById(1)).thenReturn(java.util.Optional.of(budgetItem))
        `when`(budgetItemRepository.save(any(BudgetItem::class.java))).thenAnswer { it.arguments[0] as BudgetItem }

        // when
        val result = budgetItemService.overrideForMonth(1, request)

        // then
        assertEquals(BigDecimal.ONE, result.amount)
        assertEquals(com.fairshare.model.Frequency.ONE_TIME, result.frequency)
        assertEquals(LocalDate.of(2025, 6, 1), result.startDate)
    }

    @Test
    fun `createCategoryCorrection should create a new correction`() {
        // given
        val category = Category(1, "Category", BudgetItemType.EXPENSE, 1)
        val request = CategoryCorrectionRequest(
            categoryId = 1,
            month = "2025-01",
            actualAmount = BigDecimal("150")
        )
        val plannedItems = listOf(
            BudgetItem(id = 1, name = "Item 1", amount = BigDecimal("100"), type = BudgetItemType.EXPENSE, category = category, startDate = LocalDate.of(2025,1,1))
        )
        `when`(categoryRepository.findById(1)).thenReturn(java.util.Optional.of(category))
        `when`(budgetItemRepository.findPlannedForCategoryAndMonth(BudgetItemType.EXPENSE, 1, null, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))).thenReturn(plannedItems)
        `when`(budgetItemRepository.findUnplannedForCategoryAndMonth(BudgetItemType.EXPENSE, 1, null, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31))).thenReturn(emptyList())
        `when`(budgetItemRepository.save(any(BudgetItem::class.java))).thenAnswer { it.arguments[0] as BudgetItem }

        // when
        val result = budgetItemService.createCategoryCorrection(request)

        // then
        assertEquals(BigDecimal("50"), result?.amount)
    }

    @Test
    fun `list should mark suspended items for a month`() {
        val month = "2025-02"
        val monthStart = LocalDate.of(2025, 2, 1)
        val monthEnd = LocalDate.of(2025, 2, 28)
        val item = BudgetItem(
            id = 10,
            name = "Rent",
            amount = BigDecimal("800"),
            type = BudgetItemType.EXPENSE,
            startDate = LocalDate.of(2025, 1, 1)
        )
        val suspension = BudgetItemSuspension(
            id = 5,
            budgetItem = item,
            startDate = LocalDate.of(2025, 2, 1),
            endDate = null
        )
        `when`(budgetItemRepository.findEffectiveForMonth(BudgetItemType.EXPENSE, monthStart, monthEnd))
            .thenReturn(listOf(item))
        `when`(budgetItemSuspensionRepository.findActiveForItemsAndMonth(listOf(10L), monthStart, monthEnd))
            .thenReturn(listOf(suspension))

        val result = budgetItemService.list(BudgetItemType.EXPENSE, month)

        assertEquals(1, result.size)
        assertEquals(BigDecimal.ZERO, result[0].amount)
        assertEquals(BigDecimal.ZERO, result[0].monthlyAmount)
        assertEquals(true, result[0].suspendedForMonth)
    }

    @Test
    fun `suspendExpense should create a suspension entry`() {
        val item = BudgetItem(
            id = 7,
            name = "Rent",
            amount = BigDecimal("800"),
            type = BudgetItemType.EXPENSE,
            startDate = LocalDate.of(2025, 1, 1),
            endDate = LocalDate.of(2025, 12, 31)
        )
        val request = SuspendBudgetItemRequest(startMonth = "2025-05", endMonth = "2025-06")
        `when`(budgetItemRepository.findById(7)).thenReturn(java.util.Optional.of(item))
        `when`(budgetItemSuspensionRepository.existsOverlapping(eq(7L), eq(LocalDate.of(2025, 5, 1)), eq(LocalDate.of(2025, 6, 30))))
            .thenReturn(false)
        `when`(budgetItemSuspensionRepository.save(any(BudgetItemSuspension::class.java)))
            .thenAnswer { it.arguments[0] as BudgetItemSuspension }

        val result = budgetItemService.suspendExpense(7, request)

        assertEquals(7L, result.id)
        verify(budgetItemSuspensionRepository).save(any(BudgetItemSuspension::class.java))
        verify(budgetItemRepository, never()).save(any(BudgetItem::class.java))
    }

    @Test
    fun `resumeSuspendedExpense should delete suspension when resume starts at suspension start`() {
        val item = BudgetItem(
            id = 7,
            name = "Rent",
            amount = BigDecimal("800"),
            type = BudgetItemType.EXPENSE,
            startDate = LocalDate.of(2025, 1, 1)
        )
        val suspension = BudgetItemSuspension(
            id = 11,
            budgetItem = item,
            startDate = LocalDate.of(2025, 6, 1),
            endDate = LocalDate.of(2025, 8, 31)
        )
        val request = ResumeBudgetItemRequest(startMonth = "2025-06")
        `when`(budgetItemRepository.findById(7)).thenReturn(java.util.Optional.of(item))
        `when`(
            budgetItemSuspensionRepository.findActiveForItemAndMonth(
                7L,
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 6, 30)
            )
        ).thenReturn(listOf(suspension))

        budgetItemService.resumeSuspendedExpense(7, request)

        verify(budgetItemSuspensionRepository).delete(suspension)
    }

    @Test
    fun `resumeSuspendedExpense should shorten suspension when resume starts later`() {
        val item = BudgetItem(
            id = 7,
            name = "Rent",
            amount = BigDecimal("800"),
            type = BudgetItemType.EXPENSE,
            startDate = LocalDate.of(2025, 1, 1)
        )
        val suspension = BudgetItemSuspension(
            id = 11,
            budgetItem = item,
            startDate = LocalDate.of(2025, 6, 1),
            endDate = LocalDate.of(2025, 8, 31)
        )
        val request = ResumeBudgetItemRequest(startMonth = "2025-07")
        `when`(budgetItemRepository.findById(7)).thenReturn(java.util.Optional.of(item))
        `when`(
            budgetItemSuspensionRepository.findActiveForItemAndMonth(
                7L,
                LocalDate.of(2025, 7, 1),
                LocalDate.of(2025, 7, 31)
            )
        ).thenReturn(listOf(suspension))
        `when`(budgetItemSuspensionRepository.save(any(BudgetItemSuspension::class.java)))
            .thenAnswer { it.arguments[0] as BudgetItemSuspension }

        budgetItemService.resumeSuspendedExpense(7, request)

        val captor = ArgumentCaptor.forClass(BudgetItemSuspension::class.java)
        verify(budgetItemSuspensionRepository).save(captor.capture())
        assertEquals(LocalDate.of(2025, 6, 30), captor.value.endDate)
    }

    @Test
    fun `changeValueForPeriod should split item before and after`() {
        val item = BudgetItem(
            id = 4,
            name = "Rent",
            amount = BigDecimal("800"),
            type = BudgetItemType.EXPENSE,
            frequency = Frequency.MONTHLY,
            startDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2025, 12, 31)
        )
        val request = BudgetItemValueChangeRequest(
            amount = BigDecimal("950"),
            startMonth = "2025-06",
            endMonth = "2025-08"
        )
        `when`(budgetItemRepository.findById(4)).thenReturn(java.util.Optional.of(item))
        `when`(
            budgetItemRepository.findEffectiveByRootForMonth(
                4L,
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 6, 30)
            )
        ).thenReturn(item)
        `when`(budgetItemRepository.save(any(BudgetItem::class.java))).thenAnswer { it.arguments[0] as BudgetItem }

        val result = budgetItemService.changeValueForPeriod(4, request)

        assertEquals(BigDecimal("950"), result.amount)
        val captor = ArgumentCaptor.forClass(BudgetItem::class.java)
        verify(budgetItemRepository, org.mockito.Mockito.times(3)).save(captor.capture())
        val saved = captor.allValues
        val before = saved.first { it.startDate == LocalDate.of(2024, 1, 1) }
        val changed = saved.first { it.startDate == LocalDate.of(2025, 6, 1) }
        val after = saved.first { it.startDate == LocalDate.of(2025, 9, 1) }
        assertEquals(LocalDate.of(2025, 5, 31), before.endDate)
        assertEquals(LocalDate.of(2025, 8, 31), changed.endDate)
        assertEquals(BigDecimal("800"), after.amount)
        assertEquals(LocalDate.of(2025, 12, 31), after.endDate)
    }

    @Test
    fun `changeValueForPeriod should apply to effective item for month`() {
        val root = BudgetItem(
            id = 10,
            name = "Salary",
            amount = BigDecimal("1000"),
            type = BudgetItemType.INCOME,
            frequency = Frequency.MONTHLY,
            startDate = LocalDate.of(2024, 1, 1),
            endDate = LocalDate.of(2024, 12, 31)
        )
        val future = BudgetItem(
            id = 11,
            name = "Salary",
            amount = BigDecimal("1200"),
            type = BudgetItemType.INCOME,
            frequency = Frequency.MONTHLY,
            startDate = LocalDate.of(2025, 1, 1),
            endDate = LocalDate.of(2025, 12, 31),
            rootBudgetItem = root
        )
        root.rootBudgetItem = root
        val request = BudgetItemValueChangeRequest(
            amount = BigDecimal("1400"),
            startMonth = "2025-06",
            endMonth = "2025-06"
        )
        `when`(budgetItemRepository.findById(10)).thenReturn(java.util.Optional.of(root))
        `when`(
            budgetItemRepository.findEffectiveByRootForMonth(
                10L,
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 6, 30)
            )
        ).thenReturn(future)
        `when`(budgetItemRepository.save(any(BudgetItem::class.java))).thenAnswer { it.arguments[0] as BudgetItem }

        val result = budgetItemService.changeValueForPeriod(10, request)

        assertEquals(BigDecimal("1400"), result.amount)
        assertEquals(LocalDate.of(2025, 6, 1), result.startDate)
    }
}
