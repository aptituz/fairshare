/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.BudgetItemOverrideRequest
import com.fairshare.dto.BudgetItemHistoryEntryResponse
import com.fairshare.dto.BudgetItemResponse
import com.fairshare.dto.BudgetItemValueChangeRequest
import com.fairshare.dto.CategoryCorrectionRequest
import com.fairshare.dto.CreateBudgetItemRequest
import com.fairshare.dto.ResumeBudgetItemRequest
import com.fairshare.dto.SuspendBudgetItemRequest
import com.fairshare.dto.UpdateBudgetItemRequest
import com.fairshare.exception.BadRequestException
import com.fairshare.exception.NotFoundException
import com.fairshare.mapper.toResponse
import com.fairshare.model.BudgetItem
import com.fairshare.model.BudgetItemSuspension
import com.fairshare.model.BudgetItemType
import com.fairshare.model.Frequency
import com.fairshare.repo.BudgetItemRepository
import com.fairshare.repo.BudgetItemSuspensionRepository
import com.fairshare.repo.CategoryRepository
import com.fairshare.repo.PersonRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Service
class BudgetItemService(
    private val budgetItemRepository: BudgetItemRepository,
    private val budgetItemSuspensionRepository: BudgetItemSuspensionRepository,
    private val categoryRepository: CategoryRepository,
    private val personRepository: PersonRepository,
) {
    fun list(
        type: BudgetItemType?,
        month: String?,
    ): List<BudgetItemResponse> {
        if (month != null) {
            val parsedMonth = parseYearMonth(month)
            val monthStart = parsedMonth.atDay(1)
            val monthEnd = parsedMonth.atEndOfMonth()
            val items =
                type?.let { budgetItemRepository.findEffectiveForMonth(it, monthStart, monthEnd) }
                    ?: budgetItemRepository.findEffectiveForMonth(monthStart, monthEnd)
            return applySuspensionsToResponses(items, monthStart, monthEnd)
        }
        val items = type?.let { budgetItemRepository.findByType(it) } ?: budgetItemRepository.findAll()
        return items.map { it.toResponse() }
    }

    fun historyWithSuspensions(id: Long): List<BudgetItemHistoryEntryResponse> {
        val budgetItem =
            budgetItemRepository.findById(id).orElseThrow {
                NotFoundException("Budget item $id not found")
            }
        val rootId =
            budgetItem.rootBudgetItem?.id
                ?: budgetItem.id
                ?: throw BadRequestException("Budget item $id is missing an id")
        val items = budgetItemRepository.findHistoryByRootId(rootId)
        val itemIds = items.mapNotNull { it.id }
        val suspensions =
            if (itemIds.isEmpty()) {
                emptyList()
            } else {
                budgetItemSuspensionRepository.findByBudgetItemIdIn(itemIds)
            }
        val itemEntries =
            items.map { item ->
                BudgetItemHistoryEntryResponse(
                    id = item.id,
                    name = item.name,
                    amount = item.amount,
                    type = item.type,
                    frequency = item.frequency,
                    planned = item.planned,
                    categoryCorrection = item.categoryCorrection,
                    startDate = item.startDate,
                    endDate = item.endDate,
                    previousBudgetItemId = item.previousBudgetItem?.id,
                    rootBudgetItemId = item.rootBudgetItem?.id,
                    category = item.category?.toResponse(),
                    person = item.person?.toResponse(),
                    isSuspension = false,
                    suspensionId = null,
                )
            }
        val suspensionEntries =
            suspensions.map { suspension ->
                val item = suspension.budgetItem
                BudgetItemHistoryEntryResponse(
                    id = item.id,
                    name = item.name,
                    amount = java.math.BigDecimal.ZERO,
                    type = item.type,
                    frequency = Frequency.MONTHLY,
                    planned = item.planned,
                    categoryCorrection = item.categoryCorrection,
                    startDate = suspension.startDate,
                    endDate = suspension.endDate,
                    previousBudgetItemId = item.previousBudgetItem?.id,
                    rootBudgetItemId = item.rootBudgetItem?.id,
                    category = item.category?.toResponse(),
                    person = item.person?.toResponse(),
                    isSuspension = true,
                    suspensionId = suspension.id,
                )
            }
        return (itemEntries + suspensionEntries)
            .sortedWith(
                compareBy<BudgetItemHistoryEntryResponse> { it.startDate }
                    .thenBy { if (it.isSuspension) 1 else 0 }
                    .thenBy { it.id ?: 0L },
            )
    }

    fun deleteSuspension(id: Long) {
        val suspension =
            budgetItemSuspensionRepository.findById(id).orElseThrow {
                NotFoundException("Budget item suspension $id not found")
            }
        budgetItemSuspensionRepository.delete(suspension)
    }

    fun create(request: CreateBudgetItemRequest): BudgetItemResponse {
        val category =
            request.categoryId?.let { id ->
                categoryRepository.findById(id).orElseThrow {
                    NotFoundException("Category $id not found")
                }
            }
        val person =
            request.personId?.let { id ->
                personRepository.findById(id).orElseThrow {
                    NotFoundException("Person $id not found")
                }
            }

        val frequency = request.frequency ?: Frequency.MONTHLY
        val startDate = request.startDate ?: LocalDate.now()
        val endDate = resolveEndDate(frequency, startDate, request.endDate)
        val planned = request.planned ?: true
        val saved =
            saveWithSelfRoot(
                BudgetItem(
                    name = request.name.trim(),
                    amount = request.amount,
                    type = request.type,
                    frequency = frequency,
                    planned = planned,
                    categoryCorrection = false,
                    startDate = startDate,
                    endDate = endDate,
                    category = category,
                    person = person,
                ),
            )

        return saved.toResponse()
    }

    fun update(
        id: Long,
        request: UpdateBudgetItemRequest,
    ): BudgetItemResponse {
        val budgetItem =
            budgetItemRepository.findById(id).orElseThrow {
                NotFoundException("Budget item $id not found")
            }
        val name = request.name.trim()
        if (name.isBlank()) {
            throw BadRequestException("Budget item name cannot be blank")
        }
        val category =
            request.categoryId?.let { categoryId ->
                categoryRepository.findById(categoryId).orElseThrow {
                    NotFoundException("Category $categoryId not found")
                }
            }
        val person =
            request.personId?.let { personId ->
                personRepository.findById(personId).orElseThrow {
                    NotFoundException("Person $personId not found")
                }
            }
        budgetItem.name = name
        budgetItem.amount = request.amount
        budgetItem.category = category
        budgetItem.person = person
        if (request.frequency != null) {
            budgetItem.frequency = request.frequency
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

    fun overrideForMonth(
        id: Long,
        request: BudgetItemOverrideRequest,
    ): BudgetItemResponse {
        val budgetItem =
            budgetItemRepository.findById(id).orElseThrow {
                NotFoundException("Budget item $id not found")
            }
        val month =
            try {
                YearMonth.parse(request.month)
            } catch (ex: Exception) {
                throw BadRequestException("Invalid month format. Use YYYY-MM.")
            }
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()
        val amount = request.amount

        val withinRange =
            !budgetItem.startDate.isAfter(monthEnd) &&
                (budgetItem.endDate == null || !budgetItem.endDate!!.isBefore(monthStart))
        if (!withinRange) {
            throw BadRequestException("Override month is outside the item range.")
        }

        if (budgetItem.frequency == Frequency.ONE_TIME) {
            if (budgetItem.startDate.isBefore(monthStart) || budgetItem.startDate.isAfter(monthEnd)) {
                throw BadRequestException(
                    "One-time item is not scheduled in the selected month.",
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
                    planned = budgetItem.planned,
                    startDate = monthEnd.plusDays(1),
                    endDate = originalEnd,
                    category = budgetItem.category,
                    person = budgetItem.person,
                    previousBudgetItem = budgetItem,
                    rootBudgetItem = resolveRootBudgetItem(budgetItem),
                ),
            )
        }

        val overrideItem =
            budgetItemRepository.save(
                BudgetItem(
                    name = budgetItem.name,
                    amount = amount,
                    type = budgetItem.type,
                    frequency = Frequency.ONE_TIME,
                    planned = budgetItem.planned,
                    startDate = monthStart,
                    endDate = monthEnd,
                    category = budgetItem.category,
                    person = budgetItem.person,
                    previousBudgetItem = budgetItem,
                    rootBudgetItem = resolveRootBudgetItem(budgetItem),
                ),
            )

        return overrideItem.toResponse()
    }

    fun changeValueForPeriod(
        id: Long,
        request: BudgetItemValueChangeRequest,
    ): BudgetItemResponse {
        val baseItem =
            budgetItemRepository.findById(id).orElseThrow {
                NotFoundException("Budget item $id not found")
            }
        val startMonth = parseYearMonth(request.startMonth)
        val startDate = startMonth.atDay(1)
        val endDate = request.endMonth?.let { parseYearMonth(it).atEndOfMonth() }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw BadRequestException("End month must be after start month")
        }
        val effectiveItem =
            baseItem.rootBudgetItem?.id?.let { rootId ->
                val monthStart = startMonth.atDay(1)
                val monthEnd = startMonth.atEndOfMonth()
                budgetItemRepository.findEffectiveByRootForMonth(rootId, monthStart, monthEnd)
            } ?: baseItem
        val budgetItem = effectiveItem ?: baseItem
        val originalEnd = budgetItem.endDate
        if (startDate.isBefore(budgetItem.startDate)) {
            throw BadRequestException("Change start must be within the item range")
        }
        if (originalEnd != null && startDate.isAfter(originalEnd)) {
            throw BadRequestException("Change start must be within the item range")
        }
        if (endDate != null && originalEnd != null && endDate.isAfter(originalEnd)) {
            throw BadRequestException("Change end must be within the item range")
        }

        val hasBefore = budgetItem.startDate.isBefore(startDate)
        val hasAfter =
            if (endDate == null) {
                false
            } else {
                originalEnd == null || originalEnd.isAfter(endDate)
            }
        val newEndDate = endDate ?: originalEnd
        val originalAmount = budgetItem.amount
        val originalFrequency = budgetItem.frequency

        if (!hasBefore) {
            budgetItem.amount = request.amount
            budgetItem.startDate = startDate
            budgetItem.endDate = newEndDate
            val saved = budgetItemRepository.save(budgetItem)
            if (hasAfter && newEndDate != null) {
                budgetItemRepository.save(
                    BudgetItem(
                        name = budgetItem.name,
                        amount = originalAmount,
                        type = budgetItem.type,
                        frequency = originalFrequency,
                        planned = budgetItem.planned,
                        categoryCorrection = false,
                        startDate = newEndDate.plusDays(1),
                        endDate = originalEnd,
                        category = budgetItem.category,
                        person = budgetItem.person,
                        previousBudgetItem = budgetItem,
                        rootBudgetItem = resolveRootBudgetItem(budgetItem),
                    ),
                )
            }
            return saved.toResponse()
        }

        budgetItem.endDate = startDate.minusDays(1)
        budgetItemRepository.save(budgetItem)

        val changedItem =
            budgetItemRepository.save(
                BudgetItem(
                    name = budgetItem.name,
                    amount = request.amount,
                    type = budgetItem.type,
                    frequency = budgetItem.frequency,
                    planned = budgetItem.planned,
                    categoryCorrection = false,
                    startDate = startDate,
                    endDate = newEndDate,
                    category = budgetItem.category,
                    person = budgetItem.person,
                    previousBudgetItem = budgetItem,
                    rootBudgetItem = resolveRootBudgetItem(budgetItem),
                ),
            )

        if (hasAfter && newEndDate != null) {
            budgetItemRepository.save(
                BudgetItem(
                    name = budgetItem.name,
                    amount = originalAmount,
                    type = budgetItem.type,
                    frequency = originalFrequency,
                    planned = budgetItem.planned,
                    categoryCorrection = false,
                    startDate = newEndDate.plusDays(1),
                    endDate = originalEnd,
                    category = budgetItem.category,
                    person = budgetItem.person,
                    previousBudgetItem = changedItem,
                    rootBudgetItem = resolveRootBudgetItem(budgetItem),
                ),
            )
        }

        return changedItem.toResponse()
    }

    fun suspendExpense(
        id: Long,
        request: SuspendBudgetItemRequest,
    ): BudgetItemResponse {
        val budgetItem =
            budgetItemRepository.findById(id).orElseThrow {
                NotFoundException("Budget item $id not found")
            }
        if (budgetItem.type != BudgetItemType.EXPENSE) {
            throw BadRequestException("Only expense items can be suspended")
        }
        val startMonth = parseYearMonth(request.startMonth)
        val startDate = startMonth.atDay(1)
        val endDate = request.endMonth?.let { parseYearMonth(it).atEndOfMonth() }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw BadRequestException("End month must be after start month")
        }
        if (startDate.isBefore(budgetItem.startDate)) {
            throw BadRequestException("Suspend start must be within the item range")
        }
        val originalEnd = budgetItem.endDate
        if (originalEnd != null && startDate.isAfter(originalEnd)) {
            throw BadRequestException("Suspend start must be within the item range")
        }
        if (endDate != null && originalEnd != null && endDate.isAfter(originalEnd)) {
            throw BadRequestException("Suspend end must be within the item range")
        }

        val effectiveEnd = endDate ?: LocalDate.of(9999, 12, 31)
        if (budgetItemSuspensionRepository.existsOverlapping(budgetItem.id!!, startDate, effectiveEnd)) {
            throw BadRequestException("Suspend period overlaps with an existing suspension")
        }
        budgetItemSuspensionRepository.save(
            BudgetItemSuspension(
                budgetItem = budgetItem,
                startDate = startDate,
                endDate = endDate,
            ),
        )

        return budgetItem.toResponse()
    }

    fun resumeSuspendedExpense(
        id: Long,
        request: ResumeBudgetItemRequest,
    ): BudgetItemResponse {
        val budgetItem =
            budgetItemRepository.findById(id).orElseThrow {
                NotFoundException("Budget item $id not found")
            }
        if (budgetItem.type != BudgetItemType.EXPENSE) {
            throw BadRequestException("Only expense items can be resumed")
        }
        val resumeMonth = parseYearMonth(request.startMonth)
        val resumeStart = resumeMonth.atDay(1)
        val resumeEnd = resumeMonth.atEndOfMonth()
        val suspensions =
            budgetItemSuspensionRepository.findActiveForItemAndMonth(
                budgetItem.id!!,
                resumeStart,
                resumeEnd,
            )
        val suspension = suspensions.firstOrNull()
            ?: throw BadRequestException("No suspension found for the selected month")
        if (resumeStart.isEqual(suspension.startDate)) {
            budgetItemSuspensionRepository.delete(suspension)
            return budgetItem.toResponse()
        }
        suspension.endDate = resumeStart.minusDays(1)
        budgetItemSuspensionRepository.save(suspension)
        return budgetItem.toResponse()
    }

    fun delete(id: Long) {
        val budgetItem =
            budgetItemRepository.findById(id).orElseThrow {
                NotFoundException("Budget item $id not found")
            }
        budgetItemRepository.delete(budgetItem)
    }

    fun createCategoryCorrection(request: CategoryCorrectionRequest): BudgetItemResponse? {
        val category =
            categoryRepository.findById(request.categoryId).orElseThrow {
                NotFoundException("Category ${request.categoryId} not found")
            }
        if (category.type != BudgetItemType.EXPENSE) {
            throw BadRequestException("Category must be an expense category")
        }
        val person =
            request.personId?.let { id ->
                personRepository.findById(id).orElseThrow {
                    NotFoundException("Person $id not found")
                }
            }
        val month =
            try {
                YearMonth.parse(request.month)
            } catch (ex: Exception) {
                throw BadRequestException("Invalid month format. Use YYYY-MM.")
            }
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()

        val plannedItems =
            budgetItemRepository.findPlannedForCategoryAndMonth(
                BudgetItemType.EXPENSE,
                category.id!!,
                person?.id,
                monthStart,
                monthEnd,
            )
        val plannedTotal = sumMonthlyAmounts(plannedItems)
        val correctionAmount = request.actualAmount.subtract(plannedTotal)

        val corrections =
            budgetItemRepository.findUnplannedForCategoryAndMonth(
                BudgetItemType.EXPENSE,
                category.id!!,
                person?.id,
                monthStart,
                monthEnd,
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

        val saved =
            saveWithSelfRoot(
                BudgetItem(
                    name = "Korrektur ${category.name}",
                    amount = correctionAmount,
                    type = BudgetItemType.EXPENSE,
                    frequency = Frequency.ONE_TIME,
                    planned = false,
                    categoryCorrection = true,
                    startDate = monthStart,
                    endDate = monthEnd,
                    category = category,
                    person = person,
                ),
            )
        return saved.toResponse()
    }

    private fun resolveRootBudgetItem(budgetItem: BudgetItem): BudgetItem = budgetItem.rootBudgetItem ?: budgetItem

    private fun applySuspensionsToResponses(
        items: List<BudgetItem>,
        monthStart: LocalDate,
        monthEnd: LocalDate,
    ): List<BudgetItemResponse> {
        val itemIds = items.mapNotNull { it.id }
        if (itemIds.isEmpty()) {
            return items.map { it.toResponse() }
        }
        val suspensions =
            budgetItemSuspensionRepository.findActiveForItemsAndMonth(itemIds, monthStart, monthEnd)
        val suspendedIds = suspensions.mapNotNull { it.budgetItem.id }.toSet()
        return items.map { item ->
            val response = item.toResponse()
            if (item.id != null && suspendedIds.contains(item.id)) {
                response.copy(
                    amount = BigDecimal.ZERO,
                    monthlyAmount = BigDecimal.ZERO,
                    suspendedForMonth = true,
                )
            } else {
                response
            }
        }
    }

    private fun saveWithSelfRoot(budgetItem: BudgetItem): BudgetItem {
        val saved = budgetItemRepository.save(budgetItem)
        if (saved.rootBudgetItem == null) {
            saved.rootBudgetItem = saved
            return budgetItemRepository.save(saved)
        }
        return saved
    }
}

private fun sumMonthlyAmounts(items: Iterable<BudgetItem>): java.math.BigDecimal =
    items.fold(java.math.BigDecimal.ZERO) { acc, budgetItem -> acc.add(budgetItem.monthlyAmount()) }

private fun resolveEndDate(
    frequency: Frequency,
    startDate: LocalDate,
    requestedEndDate: LocalDate?,
): LocalDate? =
    if (frequency == Frequency.ONE_TIME) {
        requestedEndDate ?: YearMonth.from(startDate).atEndOfMonth()
    } else {
        requestedEndDate
    }

private fun parseYearMonth(value: String): YearMonth =
    try {
        YearMonth.parse(value)
    } catch (ex: Exception) {
        throw BadRequestException("Invalid month format. Use YYYY-MM.")
    }
