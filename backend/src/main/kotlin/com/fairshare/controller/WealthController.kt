/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.controller

import com.fairshare.dto.CreateSavingsAccountBalanceRequest
import com.fairshare.dto.SavingsAccountBalanceResponse
import com.fairshare.dto.SavingsAccountBalanceSummaryResponse
import com.fairshare.exception.BadRequestException
import com.fairshare.service.SavingsAccountBalanceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth

@RestController
@RequestMapping("/api/wealth")
@Tag(name = "Wealth", description = "Track savings account balances over time.")
class WealthController(
    private val savingsAccountBalanceService: SavingsAccountBalanceService,
) {
    @PostMapping("/accounts/{id}/balances")
    @Operation(summary = "Record a balance for a savings account")
    fun createBalance(
        @PathVariable id: Long,
        @RequestBody request: CreateSavingsAccountBalanceRequest,
    ): SavingsAccountBalanceResponse = savingsAccountBalanceService.create(id, request)

    @GetMapping("/summary")
    @Operation(summary = "Get total savings balance per month")
    fun summary(
        @Parameter(description = "Start month (YYYY-MM)")
        @RequestParam from: String,
        @Parameter(description = "End month (YYYY-MM)")
        @RequestParam to: String,
    ): List<SavingsAccountBalanceSummaryResponse> {
        val fromMonth = parseYearMonth(from)
        val toMonth = parseYearMonth(to)
        if (fromMonth.isAfter(toMonth)) {
            throw BadRequestException("From month must be before to month")
        }
        return savingsAccountBalanceService.monthlySummary(fromMonth, toMonth)
    }

    @GetMapping("/balances")
    @Operation(summary = "List recorded savings account balances")
    fun balances(): List<SavingsAccountBalanceResponse> =
        savingsAccountBalanceService.listBalances()
}

private fun parseYearMonth(value: String): YearMonth =
    try {
        YearMonth.parse(value)
    } catch (ex: Exception) {
        throw BadRequestException("Invalid month format. Use YYYY-MM.")
    }
