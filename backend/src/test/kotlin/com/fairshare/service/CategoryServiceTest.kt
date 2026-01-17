/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.CreateCategoryRequest
import com.fairshare.dto.UpdateCategoryRequest
import com.fairshare.exception.BadRequestException
import com.fairshare.exception.ConflictException
import com.fairshare.exception.NotFoundException
import com.fairshare.model.BudgetItemType
import com.fairshare.model.Category
import com.fairshare.repo.BudgetItemRepository
import com.fairshare.repo.CategoryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class CategoryServiceTest {

    @Mock
    lateinit var categoryRepository: CategoryRepository

    @Mock
    lateinit var budgetItemRepository: BudgetItemRepository

    @InjectMocks
    lateinit var categoryService: CategoryService

    @Test
    fun `list should return all categories`() {
        // given
        val categories = listOf(
            Category(1, "Category 1", BudgetItemType.INCOME, 1),
            Category(2, "Category 2", BudgetItemType.EXPENSE, 2)
        )
        `when`(categoryRepository.findAll()).thenReturn(categories)

        // when
        val result = categoryService.list()

        // then
        assertEquals(2, result.size)
        assertEquals(1, result[0].id)
        assertEquals("Category 1", result[0].name)
        assertEquals(BudgetItemType.INCOME, result[0].type)
        assertEquals(1, result[0].rank)
        assertEquals(2, result[1].id)
        assertEquals("Category 2", result[1].name)
        assertEquals(BudgetItemType.EXPENSE, result[1].type)
        assertEquals(2, result[1].rank)
    }

    @Test
    fun `create should save and return a new category`() {
        // given
        val request = CreateCategoryRequest("New Category", BudgetItemType.INCOME)
        val category = Category(1, "New Category", BudgetItemType.INCOME, 1)
        `when`(categoryRepository.findTopByOrderByRankDesc()).thenReturn(null)
        `when`(categoryRepository.save(any(Category::class.java) ?: category)).thenReturn(category)

        // when
        val result = categoryService.create(request)

        // then
        assertEquals(1, result.id)
        assertEquals("New Category", result.name)
        assertEquals(BudgetItemType.INCOME, result.type)
        assertEquals(1, result.rank)
    }

    @Test
    fun `update should save and return an updated category`() {
        // given
        val request = UpdateCategoryRequest("Updated Category")
        val category = Category(1, "Old Category", BudgetItemType.INCOME, 1)
        `when`(categoryRepository.findById(1)).thenReturn(java.util.Optional.of(category))
        `when`(categoryRepository.save(any(Category::class.java) ?: category)).thenAnswer { it.arguments[0] as Category }

        // when
        val result = categoryService.update(1, request)

        // then
        assertEquals(1, result.id)
        assertEquals("Updated Category", result.name)
    }

    @Test
    fun `update should throw an exception when category is not found`() {
        // given
        val request = UpdateCategoryRequest("Updated Category")
        `when`(categoryRepository.findById(1)).thenReturn(java.util.Optional.empty())

        // when / then
        assertThrows(NotFoundException::class.java) {
            categoryService.update(1, request)
        }
    }

    @Test
    fun `update should throw an exception when name is blank`() {
        // given
        val request = UpdateCategoryRequest(" ")
        val category = Category(1, "Old Category", BudgetItemType.INCOME, 1)
        `when`(categoryRepository.findById(1)).thenReturn(java.util.Optional.of(category))

        // when / then
        assertThrows(BadRequestException::class.java) {
            categoryService.update(1, request)
        }
    }

    @Test
    fun `delete should remove a category when it is not used by budget items`() {
        // given
        val category = Category(1, "Test Category", BudgetItemType.INCOME, 1)
        `when`(categoryRepository.findById(1)).thenReturn(java.util.Optional.of(category))
        `when`(budgetItemRepository.existsByCategoryId(1)).thenReturn(false)

        // when
        categoryService.delete(1)

        // then
        // No exception should be thrown
    }

    @Test
    fun `delete should throw an exception when category is used by budget items`() {
        // given
        val category = Category(1, "Test Category", BudgetItemType.INCOME, 1)
        `when`(categoryRepository.findById(1)).thenReturn(java.util.Optional.of(category))
        `when`(budgetItemRepository.existsByCategoryId(1)).thenReturn(true)

        // when / then
        assertThrows(ConflictException::class.java) {
            categoryService.delete(1)
        }
    }

    @Test
    fun `delete should throw an exception when category is not found`() {
        // given
        `when`(categoryRepository.findById(1)).thenReturn(java.util.Optional.empty())

        // when / then
        assertThrows(NotFoundException::class.java) {
            categoryService.delete(1)
        }
    }
}
