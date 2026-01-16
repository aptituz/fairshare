/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.controller

import com.fairshare.dto.MonthlySummaryResponse
import com.fairshare.dto.YearlyExpenseSummaryResponse
import com.fairshare.dto.YearlySummaryResponse
import com.fairshare.service.BudgetService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.YearMonth

@RestController
@RequestMapping("/api/budget")
@Tag(name = "Budget", description = "Budget summaries and analytics.")
class BudgetController(
    private val budgetService: BudgetService,
) {
    @GetMapping("/monthly-summary")
    @Transactional(readOnly = true)
    @Operation(summary = "Get monthly budget summary")
    fun monthlySummary(
        @Parameter(description = "Target month in YYYY-MM format", example = "2026-01")
        @RequestParam(required = false)
        month: String?,
    ): MonthlySummaryResponse {
        val targetMonth =
            try {
                month?.let { YearMonth.parse(it) } ?: YearMonth.now()
            } catch (ex: Exception) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid month format. Use YYYY-MM.")
            }
        return budgetService.monthlySummary(targetMonth)
    }

    @GetMapping("/yearly-summary")
    @Transactional(readOnly = true)
    @Operation(summary = "Get yearly budget summary")
    fun yearlySummary(
        @Parameter(description = "Target year", example = "2026")
        @RequestParam(required = false)
        year: Int?,
    ): YearlySummaryResponse {
        val targetYear = year ?: YearMonth.now().year
        return budgetService.yearlySummary(targetYear)
    }

    @GetMapping("/expenses/yearly")
    @Transactional(readOnly = true)
    @Operation(summary = "Get yearly expense totals for a person or shared expenses")
    fun yearlyExpenses(
        @Parameter(description = "Target year", example = "2026")
        @RequestParam(required = false)
        year: Int?,
        @Parameter(description = "Person id (omit for shared expenses)")
        @RequestParam(required = false)
        personId: Long?,
    ): YearlyExpenseSummaryResponse {
        val targetYear = year ?: YearMonth.now().year
        return budgetService.yearlyExpenseSummary(targetYear, personId)
    }
}
