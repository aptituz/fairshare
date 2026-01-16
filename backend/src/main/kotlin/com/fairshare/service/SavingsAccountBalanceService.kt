/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.CreateSavingsAccountBalanceRequest
import com.fairshare.dto.CreateSavingsAccountBalancesRequest
import com.fairshare.dto.SavingsAccountBalanceMonthResponse
import com.fairshare.dto.SavingsAccountBalanceResponse
import com.fairshare.dto.SavingsAccountBalanceSummaryResponse
import com.fairshare.exception.BadRequestException
import com.fairshare.exception.NotFoundException
import com.fairshare.mapper.toResponse
import com.fairshare.model.SavingsAccount
import com.fairshare.model.SavingsAccountBalance
import com.fairshare.repo.SavingsAccountBalanceRepository
import com.fairshare.repo.SavingsAccountRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@Service
class SavingsAccountBalanceService(
    private val savingsAccountRepository: SavingsAccountRepository,
    private val savingsAccountBalanceRepository: SavingsAccountBalanceRepository,
    private val budgetService: BudgetService,
) {
    private data class SummaryContext(
        val accountIds: MutableList<Long>,
        val balancesByAccount: MutableMap<Long, MutableList<SavingsAccountBalance>>,
        val months: MutableList<YearMonth>,
        val householdBalances: MutableMap<String, BigDecimal>,
    )

    fun create(
        accountId: Long,
        request: CreateSavingsAccountBalanceRequest,
    ): SavingsAccountBalanceResponse {
        val account = requireAccount(accountId)
        val saved = saveBalance(account, request.balanceDate, request.balanceAmount)
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
            val account = accounts[input.savingsAccountId]
                ?: throw NotFoundException("Savings account ${input.savingsAccountId} not found")
            val saved = saveBalance(account, request.balanceDate, input.balanceAmount)
            saved.toResponse()
        }
    }

    fun listBalances(): List<SavingsAccountBalanceResponse> =
        savingsAccountBalanceRepository.findAllByOrderByBalanceDateDescIdDesc().map { it.toResponse() }

    fun monthlyBalances(year: Int): List<SavingsAccountBalanceMonthResponse> {
        val range = yearRange(year)
        val summaries = monthlySummary(range.first, range.second)
        val balances =
            savingsAccountBalanceRepository.findByBalanceDateBetweenOrderByBalanceDateDescIdDesc(
                range.first.atDay(1),
                range.second.atEndOfMonth(),
            )
        val balancesByMonth = balances.groupBy { YearMonth.from(it.balanceDate).toString() }
        var previousTotal: BigDecimal? = null
        return summaries.map { summary ->
            val monthBalances = balancesByMonth[summary.month].orEmpty().map { it.toResponse() }
            val actualMonthlySavings =
                previousTotal?.let { summary.totalBalance.subtract(it) }
            SavingsAccountBalanceMonthResponse(
                month = summary.month,
                totalBalance = summary.totalBalance,
                expectedMonthlySavings = summary.expectedMonthlySavings,
                actualMonthlySavings = actualMonthlySavings,
                balances = monthBalances,
            ).also { previousTotal = summary.totalBalance }
        }
    }

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
        val context = buildSummaryContext(from, to)
        if (context.accountIds.isEmpty()) {
            return emptyList()
        }
        return buildMonthlySummaries(context)
    }

    private fun requireAccount(accountId: Long) =
        savingsAccountRepository.findById(accountId).orElseThrow {
            NotFoundException("Savings account $accountId not found")
        }

    private fun saveBalance(
        account: SavingsAccount,
        balanceDate: LocalDate,
        amount: BigDecimal,
    ): SavingsAccountBalance {
        if (amount < BigDecimal.ZERO) {
            throw BadRequestException("Balance cannot be negative")
        }
        val existing =
            savingsAccountBalanceRepository.findBySavingsAccountIdAndBalanceDate(
                account.id ?: throw NotFoundException("Savings account must be persisted"),
                balanceDate,
            )
        return if (existing != null) {
            existing.balanceAmount = amount
            savingsAccountBalanceRepository.save(existing)
        } else {
            savingsAccountBalanceRepository.save(
                SavingsAccountBalance(
                    savingsAccount = account,
                    balanceDate = balanceDate,
                    balanceAmount = amount,
                ),
            )
        }
    }

    private fun yearRange(year: Int): Pair<YearMonth, YearMonth> =
        YearMonth.of(year, 1) to YearMonth.of(year, 12)

    private fun buildSummaryContext(
        from: YearMonth,
        to: YearMonth,
    ): SummaryContext {
        val accounts = savingsAccountRepository.findAll()
        val accountIds = accounts.mapNotNull { it.id }.toMutableList()
        val balancesByAccount = mutableMapOf<Long, MutableList<SavingsAccountBalance>>()
        if (accountIds.isNotEmpty()) {
            val balances =
                savingsAccountBalanceRepository.findBySavingsAccountIdsUpToDate(
                    accountIds,
                    to.atEndOfMonth(),
                )
            balances.forEach { balance ->
                val list = balancesByAccount.getOrPut(balance.savingsAccount.id!!) { mutableListOf() }
                list.add(balance)
            }
            balancesByAccount.values.forEach { it.sortBy { item -> item.balanceDate } }
        }
        val months = mutableListOf<YearMonth>()
        var cursor = from
        while (!cursor.isAfter(to)) {
            months.add(cursor)
            cursor = cursor.plusMonths(1)
        }
        val householdBalances = mutableMapOf<String, BigDecimal>()
        budgetService
            .monthlyTotals(from, to)
            .forEach { total -> householdBalances[total.month] = total.householdBudgetBalance }
        return SummaryContext(accountIds, balancesByAccount, months, householdBalances)
    }

    private fun buildMonthlySummaries(
        context: SummaryContext,
    ): List<SavingsAccountBalanceSummaryResponse> {
        val indices = context.accountIds.associateWith { 0 }.toMutableMap()
        val currentAmounts = mutableMapOf<Long, BigDecimal>()
        context.accountIds.forEach { accountId ->
            currentAmounts[accountId] = BigDecimal.ZERO
        }
        var expectedBalance =
            startingExpectedBalance(
                context.accountIds,
                context.balancesByAccount,
                context.months.first(),
            )

        return context.months.map { month ->
            val monthStart = month.atDay(1)
            val monthEnd = month.atEndOfMonth()
            updateCurrentAmounts(context.accountIds, context.balancesByAccount, indices, currentAmounts, monthEnd)
            val activeAccountIds = activeAccountsForMonth(monthStart, monthEnd)
            val total = sumActiveBalances(currentAmounts, activeAccountIds)
            val monthlyBalance = context.householdBalances[month.toString()] ?: BigDecimal.ZERO
            val response =
                SavingsAccountBalanceSummaryResponse(
                    month = month.toString(),
                    totalBalance = total,
                    expectedBalance = expectedBalance,
                    expectedMonthlySavings = monthlyBalance,
                )
            expectedBalance = expectedBalance.add(monthlyBalance)
            response
        }
    }

    private fun startingExpectedBalance(
        accountIds: List<Long>,
        balancesByAccount: Map<Long, MutableList<SavingsAccountBalance>>,
        firstMonth: YearMonth,
    ): BigDecimal {
        val startDate = firstMonth.atDay(1).minusDays(1)
        return accountIds.fold(BigDecimal.ZERO) { acc, accountId ->
            val items = balancesByAccount[accountId].orEmpty()
            val prior = items.lastOrNull { !it.balanceDate.isAfter(startDate) }
            acc.add(prior?.balanceAmount ?: BigDecimal.ZERO)
        }
    }

    private fun updateCurrentAmounts(
        accountIds: List<Long>,
        balancesByAccount: Map<Long, MutableList<SavingsAccountBalance>>,
        indices: MutableMap<Long, Int>,
        currentAmounts: MutableMap<Long, BigDecimal>,
        monthEnd: LocalDate,
    ) {
        accountIds.forEach { accountId ->
            val items = balancesByAccount[accountId].orEmpty()
            var index = indices[accountId] ?: 0
            while (index < items.size && !items[index].balanceDate.isAfter(monthEnd)) {
                currentAmounts[accountId] = items[index].balanceAmount
                index += 1
            }
            indices[accountId] = index
        }
    }

    private fun activeAccountsForMonth(
        monthStart: LocalDate,
        monthEnd: LocalDate,
    ): Set<Long> =
        savingsAccountRepository.findActiveIdsForMonth(monthStart, monthEnd).toSet()

    private fun sumActiveBalances(
        currentAmounts: Map<Long, BigDecimal>,
        activeAccountIds: Set<Long>,
    ): BigDecimal =
        currentAmounts
            .filter { (accountId, _) -> activeAccountIds.contains(accountId) }
            .values
            .fold(BigDecimal.ZERO) { acc, value -> acc.add(value) }
}
