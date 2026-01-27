/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.repo

import com.fairshare.model.ImportBatch
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface ImportBatchRepository : JpaRepository<ImportBatch, Long> {
    fun existsByPersonIdAndFileHash(
        personId: Long,
        fileHash: String,
    ): Boolean

    fun existsByPersonIdIsNullAndFileHash(fileHash: String): Boolean

    fun findAllByPersonIdOrderByImportDateDesc(
        personId: Long,
        pageable: Pageable,
    ): List<ImportBatch>

    fun findAllByOrderByImportDateDesc(pageable: Pageable): List<ImportBatch>
}
