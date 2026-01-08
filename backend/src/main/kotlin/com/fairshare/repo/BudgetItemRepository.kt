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
    fun findByTypeAndActiveTrue(type: BudgetItemType): List<BudgetItem>
    fun findByActiveTrue(): List<BudgetItem>
    @Query(
        """
        select b
        from BudgetItem b
        where b.active = true
          and b.type = :type
          and b.startDate <= :monthEnd
          and (b.endDate is null or b.endDate >= :monthStart)
        """
    )
    fun findActiveForMonth(
        @Param("type") type: BudgetItemType,
        @Param("monthStart") monthStart: LocalDate,
        @Param("monthEnd") monthEnd: LocalDate
    ): List<BudgetItem>
    @Query(
        """
        select b
        from BudgetItem b
        where b.active = true
          and b.planned = true
          and b.type = :type
          and b.category.id = :categoryId
          and b.startDate <= :monthEnd
          and (b.endDate is null or b.endDate >= :monthStart)
          and ((:personId is null and b.person is null) or b.person.id = :personId)
        """
    )
    fun findPlannedForCategoryAndMonth(
        @Param("type") type: BudgetItemType,
        @Param("categoryId") categoryId: Long,
        @Param("personId") personId: Long?,
        @Param("monthStart") monthStart: LocalDate,
        @Param("monthEnd") monthEnd: LocalDate
    ): List<BudgetItem>
    @Query(
        """
        select b
        from BudgetItem b
        where b.active = true
          and b.planned = false
          and b.categoryCorrection = true
          and b.type = :type
          and b.category.id = :categoryId
          and b.startDate <= :monthEnd
          and (b.endDate is null or b.endDate >= :monthStart)
          and ((:personId is null and b.person is null) or b.person.id = :personId)
        """
    )
    fun findUnplannedForCategoryAndMonth(
        @Param("type") type: BudgetItemType,
        @Param("categoryId") categoryId: Long,
        @Param("personId") personId: Long?,
        @Param("monthStart") monthStart: LocalDate,
        @Param("monthEnd") monthEnd: LocalDate
    ): List<BudgetItem>
    fun existsByCategory_Id(categoryId: Long): Boolean
}
