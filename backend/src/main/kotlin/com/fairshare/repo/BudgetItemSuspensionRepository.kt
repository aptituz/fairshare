/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.repo

import com.fairshare.model.BudgetItemSuspension
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface BudgetItemSuspensionRepository : JpaRepository<BudgetItemSuspension, Long> {
    fun findByBudgetItemIdIn(budgetItemIds: Collection<Long>): List<BudgetItemSuspension>

    @Query(
        """
        select s
        from BudgetItemSuspension s
        where s.budgetItem.id in :budgetItemIds
          and s.startDate <= :monthEnd
          and (s.endDate is null or s.endDate >= :monthStart)
        """,
    )
    fun findActiveForItemsAndMonth(
        @Param("budgetItemIds") budgetItemIds: Collection<Long>,
        @Param("monthStart") monthStart: LocalDate,
        @Param("monthEnd") monthEnd: LocalDate,
    ): List<BudgetItemSuspension>

    @Query(
        """
        select s
        from BudgetItemSuspension s
        where s.budgetItem.id = :budgetItemId
          and s.startDate <= :monthEnd
          and (s.endDate is null or s.endDate >= :monthStart)
        order by s.startDate desc
        """,
    )
    fun findActiveForItemAndMonth(
        @Param("budgetItemId") budgetItemId: Long,
        @Param("monthStart") monthStart: LocalDate,
        @Param("monthEnd") monthEnd: LocalDate,
    ): List<BudgetItemSuspension>

    @Query(
        """
        select count(s) > 0
        from BudgetItemSuspension s
        where s.budgetItem.id = :budgetItemId
          and s.startDate <= :endDate
          and (s.endDate is null or s.endDate >= :startDate)
        """,
    )
    fun existsOverlapping(
        @Param("budgetItemId") budgetItemId: Long,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
    ): Boolean
}
