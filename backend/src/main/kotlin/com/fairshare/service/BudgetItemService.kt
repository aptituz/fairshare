/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.CategoryCorrectionRequest
import com.fairshare.dto.CategoryResponse
import com.fairshare.dto.BudgetItemOverrideRequest
import com.fairshare.dto.CreateBudgetItemRequest
import com.fairshare.dto.UpdateBudgetItemRequest
import com.fairshare.dto.BudgetItemResponse
import com.fairshare.dto.PersonResponse
import com.fairshare.model.Frequency
import com.fairshare.model.BudgetItem
import com.fairshare.model.BudgetItemType
import com.fairshare.repo.CategoryRepository
import com.fairshare.repo.BudgetItemRepository
import com.fairshare.repo.PersonRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate
import java.time.YearMonth

@Service
class BudgetItemService(
    private val budgetItemRepository: BudgetItemRepository,
    private val categoryRepository: CategoryRepository,
    private val personRepository: PersonRepository
) {
    fun list(type: BudgetItemType?): List<BudgetItemResponse> =
        (type?.let { budgetItemRepository.findByType(it) } ?: budgetItemRepository.findAll())
            .map { it.toResponse() }

    fun create(request: CreateBudgetItemRequest): BudgetItemResponse {
        val category = request.categoryId?.let { id ->
            categoryRepository.findById(id).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Category $id not found")
            }
        }
        val person = request.personId?.let { id ->
            personRepository.findById(id).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Person $id not found")
            }
        }

        val frequency = request.frequency ?: Frequency.MONTHLY
        val startDate = request.startDate ?: LocalDate.now()
        val endDate = resolveEndDate(frequency, startDate, request.endDate)
        val saved = budgetItemRepository.save(
            BudgetItem(
                name = request.name.trim(),
                amount = request.amount,
                type = request.type,
                frequency = frequency,
                active = request.active ?: true,
                planned = request.planned ?: true,
                categoryCorrection = false,
                startDate = startDate,
                endDate = endDate,
                category = category,
                person = person
            )
        )

        return saved.toResponse()
    }

    fun update(id: Long, request: UpdateBudgetItemRequest): BudgetItemResponse {
        val budgetItem = budgetItemRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Budget item $id not found")
        }
        val name = request.name.trim()
        if (name.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Budget item name cannot be blank")
        }
        val category = request.categoryId?.let { categoryId ->
            categoryRepository.findById(categoryId).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Category $categoryId not found")
            }
        }
        val person = request.personId?.let { personId ->
            personRepository.findById(personId).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Person $personId not found")
            }
        }
        budgetItem.name = name
        budgetItem.amount = request.amount
        budgetItem.category = category
        budgetItem.person = person
        if (request.frequency != null) {
            budgetItem.frequency = request.frequency
        }
        if (request.active != null) {
            budgetItem.active = request.active
        }
        if (request.planned != null) {
            budgetItem.planned = request.planned
        }
        if (request.startDate != null) {
            budgetItem.startDate = request.startDate
        }
        val resolvedStartDate = request.startDate ?: budgetItem.startDate
        val resolvedFrequency = request.frequency ?: budgetItem.frequency
        budgetItem.endDate = resolveEndDate(resolvedFrequency, resolvedStartDate, request.endDate)
        return budgetItemRepository.save(budgetItem).toResponse()
    }

    fun overrideForMonth(id: Long, request: BudgetItemOverrideRequest): BudgetItemResponse {
        val budgetItem = budgetItemRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Budget item $id not found")
        }
        val month = try {
            YearMonth.parse(request.month)
        } catch (ex: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid month format. Use YYYY-MM.")
        }
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()
        val amount = request.amount

        val withinRange = !budgetItem.startDate.isAfter(monthEnd) &&
            (budgetItem.endDate == null || !budgetItem.endDate!!.isBefore(monthStart))
        if (!withinRange) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Override month is outside the item range.")
        }

        if (budgetItem.frequency == Frequency.ONE_TIME) {
            if (budgetItem.startDate.isBefore(monthStart) || budgetItem.startDate.isAfter(monthEnd)) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "One-time item is not scheduled in the selected month."
                )
            }
            budgetItem.amount = amount
            return budgetItemRepository.save(budgetItem).toResponse()
        }

        val hasBefore = budgetItem.startDate.isBefore(monthStart)
        val hasAfter = budgetItem.endDate == null || budgetItem.endDate!!.isAfter(monthEnd)
        val originalEnd = budgetItem.endDate
        val originalAmount = budgetItem.amount

        if (!hasBefore && !hasAfter) {
            budgetItem.amount = amount
            budgetItem.frequency = Frequency.ONE_TIME
            budgetItem.startDate = monthStart
            budgetItem.endDate = monthEnd
            return budgetItemRepository.save(budgetItem).toResponse()
        }

        if (hasBefore) {
            budgetItem.endDate = monthStart.minusDays(1)
            budgetItemRepository.save(budgetItem)
        } else if (hasAfter) {
            budgetItem.startDate = monthEnd.plusDays(1)
            budgetItemRepository.save(budgetItem)
        }

        if (hasBefore && hasAfter) {
            budgetItemRepository.save(
                BudgetItem(
                    name = budgetItem.name,
                    amount = originalAmount,
                    type = budgetItem.type,
                    frequency = budgetItem.frequency,
                    active = budgetItem.active,
                    startDate = monthEnd.plusDays(1),
                    endDate = originalEnd,
                    category = budgetItem.category,
                    person = budgetItem.person
                )
            )
        }

        val overrideItem = budgetItemRepository.save(
            BudgetItem(
                name = budgetItem.name,
                amount = amount,
                type = budgetItem.type,
                frequency = Frequency.ONE_TIME,
                active = budgetItem.active,
                startDate = monthStart,
                endDate = monthEnd,
                category = budgetItem.category,
                person = budgetItem.person
            )
        )

        return overrideItem.toResponse()
    }

    fun delete(id: Long) {
        val budgetItem = budgetItemRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Budget item $id not found")
        }
        budgetItemRepository.delete(budgetItem)
    }

    fun createCategoryCorrection(request: CategoryCorrectionRequest): BudgetItemResponse? {
        val category = categoryRepository.findById(request.categoryId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Category ${request.categoryId} not found")
        }
        if (category.type != BudgetItemType.EXPENSE) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Category must be an expense category")
        }
        val person = request.personId?.let { id ->
            personRepository.findById(id).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Person $id not found")
            }
        }
        val month = try {
            YearMonth.parse(request.month)
        } catch (ex: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid month format. Use YYYY-MM.")
        }
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()

        val plannedItems = budgetItemRepository.findPlannedForCategoryAndMonth(
            BudgetItemType.EXPENSE,
            category.id!!,
            person?.id,
            monthStart,
            monthEnd
        )
        val plannedTotal = sumMonthlyAmounts(plannedItems)
        val correctionAmount = request.actualAmount.subtract(plannedTotal)

        val corrections = budgetItemRepository.findUnplannedForCategoryAndMonth(
            BudgetItemType.EXPENSE,
            category.id!!,
            person?.id,
            monthStart,
            monthEnd
        )
        val existingCorrection = corrections.firstOrNull()
        if (correctionAmount.compareTo(java.math.BigDecimal.ZERO) == 0) {
            if (corrections.isNotEmpty()) {
                corrections.forEach { budgetItemRepository.delete(it) }
            }
            return null
        }
        if (corrections.isNotEmpty()) {
            corrections.forEach { budgetItemRepository.delete(it) }
        }

        val saved = budgetItemRepository.save(
            BudgetItem(
                name = "Korrektur ${category.name}",
                amount = correctionAmount,
                type = BudgetItemType.EXPENSE,
                frequency = Frequency.ONE_TIME,
                active = true,
                planned = false,
                categoryCorrection = true,
                startDate = monthStart,
                endDate = monthEnd,
                category = category,
                person = person
            )
        )
        return saved.toResponse()
    }
}

private fun sumMonthlyAmounts(items: Iterable<BudgetItem>): java.math.BigDecimal =
    items.fold(java.math.BigDecimal.ZERO) { acc, budgetItem -> acc.add(budgetItem.monthlyAmount()) }

private fun resolveEndDate(
    frequency: Frequency,
    startDate: LocalDate,
    requestedEndDate: LocalDate?
): LocalDate? {
    return if (frequency == Frequency.ONE_TIME) {
        requestedEndDate ?: YearMonth.from(startDate).atEndOfMonth()
    } else {
        requestedEndDate
    }
}

private fun BudgetItem.toResponse(): BudgetItemResponse = BudgetItemResponse(
    id = id,
    name = name,
    amount = amount,
    type = type,
    frequency = frequency,
    monthlyAmount = monthlyAmount(),
    active = active,
    planned = planned,
    categoryCorrection = categoryCorrection,
    startDate = startDate,
    endDate = endDate,
    category = category?.let {
        CategoryResponse(
            id = it.id,
            name = it.name,
            type = it.type,
            rank = it.rank
        )
    },
    person = person?.let { PersonResponse(id = it.id, name = it.name) }
)
