/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.dto

import java.time.LocalDateTime

data class ImportHistoryResponse(
    val imports: List<ImportHistoryItemResponse>,
)

data class ImportHistoryItemResponse(
    val batchId: Long,
    val personId: Long?,
    val date: LocalDateTime,
    val fileName: String,
    val count: Int?,
    val status: String,
)
