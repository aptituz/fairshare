/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.CategoryResponse
import com.fairshare.dto.CreateCategoryRequest
import com.fairshare.dto.UpdateCategoryRequest
import com.fairshare.exception.BadRequestException
import com.fairshare.exception.ConflictException
import com.fairshare.exception.NotFoundException
import com.fairshare.mapper.toResponse
import com.fairshare.model.Category
import com.fairshare.repo.BudgetItemRepository
import com.fairshare.repo.CategoryRepository
import org.springframework.stereotype.Service

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val budgetItemRepository: BudgetItemRepository,
) {
    fun list(): List<CategoryResponse> = categoryRepository.findAll().map { it.toResponse() }

    fun create(request: CreateCategoryRequest): CategoryResponse {
        val name = request.name.trim()
        val nextRank = (categoryRepository.findTopByOrderByRankDesc()?.rank ?: 0) + 1
        val saved = categoryRepository.save(Category(name = name, type = request.type, rank = nextRank))
        return saved.toResponse()
    }

    fun update(
        id: Long,
        request: UpdateCategoryRequest,
    ): CategoryResponse {
        val category =
            categoryRepository.findById(id).orElseThrow {
                NotFoundException("Category $id not found")
            }
        val name = request.name.trim()
        if (name.isBlank()) {
            throw BadRequestException("Category name cannot be blank")
        }
        category.name = name
        return categoryRepository.save(category).toResponse()
    }

    fun delete(id: Long) {
        val category =
            categoryRepository.findById(id).orElseThrow {
                NotFoundException("Category $id not found")
            }
        if (budgetItemRepository.existsByCategoryId(id)) {
            throw ConflictException("Category $id is used by budget items")
        }
        categoryRepository.delete(category)
    }
}
