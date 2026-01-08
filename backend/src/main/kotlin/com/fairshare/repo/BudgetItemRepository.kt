/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.repo

import com.fairshare.model.BudgetItem
import com.fairshare.model.BudgetItemType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface BudgetItemRepository : JpaRepository<BudgetItem, Long> {
    fun findByType(type: BudgetItemType): List<BudgetItem>

    @Query(
        """
        select b
        from BudgetItem b
        where b.type = :type
          and b.startDate <= :monthEnd
          and (b.endDate is null or b.endDate >= :monthStart)
        """,
    )
    fun findForMonth(
        @Param("type") type: BudgetItemType,
        @Param("monthStart") monthStart: LocalDate,
        @Param("monthEnd") monthEnd: LocalDate,
    ): List<BudgetItem>

    @Query(
        """
        select b
        from BudgetItem b
        where b.type = :type
          and b.startDate <= :monthEnd
          and (b.endDate is null or b.endDate >= :monthStart)
          and b.startDate = (
            select max(b2.startDate)
            from BudgetItem b2
            where b2.rootBudgetItem = b.rootBudgetItem
              and b2.type = :type
              and b2.startDate <= :monthEnd
              and (b2.endDate is null or b2.endDate >= :monthStart)
          )
          and b.id = (
            select max(b3.id)
            from BudgetItem b3
            where b3.rootBudgetItem = b.rootBudgetItem
              and b3.type = :type
              and b3.startDate = b.startDate
              and b3.startDate <= :monthEnd
              and (b3.endDate is null or b3.endDate >= :monthStart)
          )
        """,
    )
    fun findEffectiveForMonth(
        @Param("type") type: BudgetItemType,
        @Param("monthStart") monthStart: LocalDate,
        @Param("monthEnd") monthEnd: LocalDate,
    ): List<BudgetItem>

    @Query(
        """
        select b
        from BudgetItem b
        where b.startDate <= :monthEnd
          and (b.endDate is null or b.endDate >= :monthStart)
          and b.startDate = (
            select max(b2.startDate)
            from BudgetItem b2
            where b2.rootBudgetItem = b.rootBudgetItem
              and b2.startDate <= :monthEnd
              and (b2.endDate is null or b2.endDate >= :monthStart)
          )
          and b.id = (
            select max(b3.id)
            from BudgetItem b3
            where b3.rootBudgetItem = b.rootBudgetItem
              and b3.startDate = b.startDate
              and b3.startDate <= :monthEnd
              and (b3.endDate is null or b3.endDate >= :monthStart)
          )
        """,
    )
    fun findEffectiveForMonth(
        @Param("monthStart") monthStart: LocalDate,
        @Param("monthEnd") monthEnd: LocalDate,
    ): List<BudgetItem>

    @Query(
        """
        select b
        from BudgetItem b
        where b.planned = true
          and b.type = :type
          and b.category.id = :categoryId
          and b.startDate <= :monthEnd
          and (b.endDate is null or b.endDate >= :monthStart)
          and ((:personId is null and b.person is null) or b.person.id = :personId)
        """,
    )
    fun findPlannedForCategoryAndMonth(
        @Param("type") type: BudgetItemType,
        @Param("categoryId") categoryId: Long,
        @Param("personId") personId: Long?,
        @Param("monthStart") monthStart: LocalDate,
        @Param("monthEnd") monthEnd: LocalDate,
    ): List<BudgetItem>

    @Query(
        """
        select b
        from BudgetItem b
        where b.planned = false
          and b.categoryCorrection = true
          and b.type = :type
          and b.category.id = :categoryId
          and b.startDate <= :monthEnd
          and (b.endDate is null or b.endDate >= :monthStart)
          and ((:personId is null and b.person is null) or b.person.id = :personId)
        """,
    )
    fun findUnplannedForCategoryAndMonth(
        @Param("type") type: BudgetItemType,
        @Param("categoryId") categoryId: Long,
        @Param("personId") personId: Long?,
        @Param("monthStart") monthStart: LocalDate,
        @Param("monthEnd") monthEnd: LocalDate,
    ): List<BudgetItem>

    fun existsByCategoryId(categoryId: Long): Boolean

    fun existsByPersonId(personId: Long): Boolean

    @Query(
        """
        select b
        from BudgetItem b
        where b.rootBudgetItem.id = :rootId
        order by b.startDate, b.id
        """,
    )
    fun findHistoryByRootId(
        @Param("rootId") rootId: Long,
    ): List<BudgetItem>
}
