/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.dto

data class ImportTransactionsResponse(
    val success: Boolean,
    val batchId: Long,
    val status: String,
    val message: String,
)
