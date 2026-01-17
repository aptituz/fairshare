/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.repo

import com.fairshare.model.SavingsAccountBalance
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface SavingsAccountBalanceRepository : JpaRepository<SavingsAccountBalance, Long> {
    fun findBySavingsAccountIdAndBalanceDate(
        savingsAccountId: Long,
        balanceDate: LocalDate,
    ): SavingsAccountBalance?

    @Query(
        """
        select b
        from SavingsAccountBalance b
        where b.savingsAccount.id in :accountIds
          and b.balanceDate <= :endDate
        order by b.savingsAccount.id, b.balanceDate
        """,
    )
    fun findBySavingsAccountIdsUpToDate(
        @Param("accountIds") accountIds: Collection<Long>,
        @Param("endDate") endDate: LocalDate,
    ): List<SavingsAccountBalance>

    fun findAllByOrderByBalanceDateDescIdDesc(): List<SavingsAccountBalance>

    fun findByBalanceDateBetweenOrderByBalanceDateDescIdDesc(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<SavingsAccountBalance>
}
