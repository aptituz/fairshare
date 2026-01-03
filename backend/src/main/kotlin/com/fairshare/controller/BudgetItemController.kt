package com.fairshare.controller

import com.fairshare.dto.CategoryCorrectionRequest
import com.fairshare.dto.CreateBudgetItemRequest
import com.fairshare.dto.BudgetItemResponse
import com.fairshare.dto.BudgetItemOverrideRequest
import com.fairshare.dto.UpdateBudgetItemRequest
import com.fairshare.model.BudgetItemType
import com.fairshare.service.BudgetItemService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/budget-items")
@Tag(name = "Budget Items", description = "Manage recurring income and expenses.")
class BudgetItemController(
    private val budgetItemService: BudgetItemService
) {
    @GetMapping
    @Operation(summary = "List budget items")
    fun list(
        @Parameter(description = "Filter by budget item type", example = "INCOME")
        @RequestParam(required = false)
        type: BudgetItemType?
    ): List<BudgetItemResponse> =
        budgetItemService.list(type)

    @PostMapping
    @Operation(summary = "Create a budget item")
    fun create(@RequestBody request: CreateBudgetItemRequest): BudgetItemResponse {
        return budgetItemService.create(request)
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a budget item")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: UpdateBudgetItemRequest
    ): BudgetItemResponse =
        budgetItemService.update(id, request)

    @PostMapping("/{id}/month-override")
    @Operation(summary = "Override a budget item amount for a specific month")
    fun overrideMonth(
        @PathVariable id: Long,
        @RequestBody request: BudgetItemOverrideRequest
    ): BudgetItemResponse =
        budgetItemService.overrideForMonth(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a budget item")
    fun delete(@PathVariable id: Long) {
        budgetItemService.delete(id)
    }

    @PostMapping("/category-correction")
    @Operation(summary = "Create a one-time correction for a category based on actual amount")
    fun createCategoryCorrection(
        @RequestBody request: CategoryCorrectionRequest
    ): ResponseEntity<BudgetItemResponse> {
        val response = budgetItemService.createCategoryCorrection(request)
        return response?.let { ResponseEntity.ok(it) } ?: ResponseEntity.noContent().build()
    }
}
