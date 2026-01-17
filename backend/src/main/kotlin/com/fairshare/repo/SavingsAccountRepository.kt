/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.repo

import com.fairshare.model.SavingsAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface SavingsAccountRepository : JpaRepository<SavingsAccount, Long> {
    @Query(
        """
        select a.id
        from SavingsAccount a
        where (a.startDate is null or a.startDate <= :monthEnd)
          and (a.endDate is null or a.endDate >= :monthStart)
        """,
    )
    fun findActiveIdsForMonth(
        @Param("monthStart") monthStart: LocalDate,
        @Param("monthEnd") monthEnd: LocalDate,
    ): List<Long>
}
