package com.fairshare.controller

import com.fairshare.dto.MonthlySummaryResponse
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
}
