/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.CreateSavingsAccountBalanceRequest
import com.fairshare.dto.CreateSavingsAccountBalancesRequest
import com.fairshare.dto.SavingsAccountBalanceResponse
import com.fairshare.dto.SavingsAccountBalanceSummaryResponse
import com.fairshare.exception.BadRequestException
import com.fairshare.exception.NotFoundException
import com.fairshare.mapper.toResponse
import com.fairshare.model.SavingsAccountBalance
import com.fairshare.repo.SavingsAccountBalanceRepository
import com.fairshare.repo.SavingsAccountRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.YearMonth

@Service
class SavingsAccountBalanceService(
    private val savingsAccountRepository: SavingsAccountRepository,
    private val savingsAccountBalanceRepository: SavingsAccountBalanceRepository,
) {
    fun create(
        accountId: Long,
        request: CreateSavingsAccountBalanceRequest,
    ): SavingsAccountBalanceResponse {
        val account =
            savingsAccountRepository.findById(accountId).orElseThrow {
                NotFoundException("Savings account $accountId not found")
            }
        val amount = request.balanceAmount
        if (amount < BigDecimal.ZERO) {
            throw BadRequestException("Balance cannot be negative")
        }
        val existing =
            savingsAccountBalanceRepository.findBySavingsAccountIdAndBalanceDate(
                accountId,
                request.balanceDate,
            )
        val saved =
            if (existing != null) {
                existing.balanceAmount = amount
                savingsAccountBalanceRepository.save(existing)
            } else {
                savingsAccountBalanceRepository.save(
                    SavingsAccountBalance(
                        savingsAccount = account,
                        balanceDate = request.balanceDate,
                        balanceAmount = amount,
                    ),
                )
            }
        return saved.toResponse()
    }

    fun createBulk(request: CreateSavingsAccountBalancesRequest): List<SavingsAccountBalanceResponse> {
        if (request.balances.isEmpty()) {
            throw BadRequestException("Balances cannot be empty")
        }
        val accountIds = request.balances.map { it.savingsAccountId }.distinct()
        val accounts = savingsAccountRepository.findAllById(accountIds).associateBy { it.id }
        if (accounts.size != accountIds.size) {
            throw NotFoundException("One or more savings accounts not found")
        }
        return request.balances.map { input ->
            val amount = input.balanceAmount
            if (amount < BigDecimal.ZERO) {
                throw BadRequestException("Balance cannot be negative")
            }
            val account = accounts[input.savingsAccountId]
                ?: throw NotFoundException("Savings account ${input.savingsAccountId} not found")
            val existing =
                savingsAccountBalanceRepository.findBySavingsAccountIdAndBalanceDate(
                    input.savingsAccountId,
                    request.balanceDate,
                )
            val saved =
                if (existing != null) {
                    existing.balanceAmount = amount
                    savingsAccountBalanceRepository.save(existing)
                } else {
                    savingsAccountBalanceRepository.save(
                        SavingsAccountBalance(
                            savingsAccount = account,
                            balanceDate = request.balanceDate,
                            balanceAmount = amount,
                        ),
                    )
                }
            saved.toResponse()
        }
    }

    fun listBalances(): List<SavingsAccountBalanceResponse> =
        savingsAccountBalanceRepository.findAllByOrderByBalanceDateDescIdDesc().map { it.toResponse() }

    fun delete(id: Long) {
        val balance =
            savingsAccountBalanceRepository.findById(id).orElseThrow {
                NotFoundException("Savings account balance $id not found")
            }
        savingsAccountBalanceRepository.delete(balance)
    }

    fun monthlySummary(
        from: YearMonth,
        to: YearMonth,
    ): List<SavingsAccountBalanceSummaryResponse> {
        val accounts = savingsAccountRepository.findAll()
        if (accounts.isEmpty()) {
            return emptyList()
        }
        val accountIds = accounts.mapNotNull { it.id }
        val endDate = to.atEndOfMonth()
        val balances = savingsAccountBalanceRepository.findBySavingsAccountIdsUpToDate(accountIds, endDate)
        val balancesByAccount =
            balances.groupBy { it.savingsAccount.id!! }.mapValues { (_, items) ->
                items.sortedBy { it.balanceDate }
            }

        val months = mutableListOf<YearMonth>()
        var cursor = from
        while (!cursor.isAfter(to)) {
            months.add(cursor)
            cursor = cursor.plusMonths(1)
        }

        val indices = accountIds.associateWith { 0 }.toMutableMap()
        val currentAmounts = accountIds.associateWith { BigDecimal.ZERO }.toMutableMap()

        return months.map { month ->
            val monthEnd = month.atEndOfMonth()
            accountIds.forEach { accountId ->
                val items = balancesByAccount[accountId].orEmpty()
                var index = indices[accountId] ?: 0
                while (index < items.size && !items[index].balanceDate.isAfter(monthEnd)) {
                    currentAmounts[accountId] = items[index].balanceAmount
                    index += 1
                }
                indices[accountId] = index
            }
            val total = currentAmounts.values.fold(BigDecimal.ZERO) { acc, value -> acc.add(value) }
            SavingsAccountBalanceSummaryResponse(
                month = month.toString(),
                totalBalance = total,
            )
        }
    }
}
