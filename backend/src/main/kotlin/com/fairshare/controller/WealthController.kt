/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.controller

import com.fairshare.dto.CreateSavingsAccountBalanceRequest
import com.fairshare.dto.CreateSavingsAccountBalancesRequest
import com.fairshare.dto.SavingsAccountBalanceMonthResponse
import com.fairshare.dto.SavingsAccountBalanceResponse
import com.fairshare.dto.SavingsAccountBalanceSummaryResponse
import com.fairshare.exception.BadRequestException
import com.fairshare.service.SavingsAccountBalanceService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.Year
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

    @PostMapping("/balances/bulk")
    @Operation(summary = "Record balances for multiple savings accounts")
    fun createBalances(
        @RequestBody request: CreateSavingsAccountBalancesRequest,
    ): List<SavingsAccountBalanceResponse> = savingsAccountBalanceService.createBulk(request)

    @GetMapping("/summary")
    @Operation(summary = "Get total savings balance per month for a year")
    fun summary(
        @Parameter(description = "Year (YYYY)")
        @RequestParam year: String,
    ): List<SavingsAccountBalanceSummaryResponse> {
        val parsedYear = parseYear(year)
        val fromMonth = YearMonth.of(parsedYear, 1)
        val toMonth = YearMonth.of(parsedYear, 12)
        return savingsAccountBalanceService.monthlySummary(fromMonth, toMonth)
    }

    @GetMapping("/balances")
    @Operation(summary = "List recorded savings account balances")
    fun balances(): List<SavingsAccountBalanceResponse> = savingsAccountBalanceService.listBalances()

    @GetMapping("/balances/monthly")
    @Operation(summary = "List savings account balances grouped by month for a year")
    fun balancesByMonth(
        @Parameter(description = "Year (YYYY)")
        @RequestParam year: String,
    ): List<SavingsAccountBalanceMonthResponse> = savingsAccountBalanceService.monthlyBalances(parseYear(year))

    @DeleteMapping("/balances/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a recorded balance entry")
    fun deleteBalance(
        @PathVariable id: Long,
    ) {
        savingsAccountBalanceService.delete(id)
    }
}

private fun parseYear(value: String): Int =
    try {
        Year.parse(value).value
    } catch (ex: Exception) {
        throw BadRequestException("Invalid year format. Use YYYY.")
    }
