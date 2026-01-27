/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.repo

import com.fairshare.model.Transaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TransactionRepository : JpaRepository<Transaction, Long> {
    fun existsByDeduplicationKey(deduplicationKey: String): Boolean

    fun countByImportBatchId(importBatchId: Long): Long

    @Modifying
    @Query("delete from Transaction t where t.importBatch.id = :importBatchId")
    fun deleteByImportBatchId(
        @Param("importBatchId") importBatchId: Long,
    ): Int
}
