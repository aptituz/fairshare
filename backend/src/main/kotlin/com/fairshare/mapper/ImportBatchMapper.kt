/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.mapper

import com.fairshare.dto.ImportHistoryItemResponse
import com.fairshare.model.ImportBatch

fun ImportBatch.toHistoryItem(): ImportHistoryItemResponse =
    ImportHistoryItemResponse(
        batchId = id ?: 0,
        personId = person?.id,
        date = importDate,
        fileName = fileName,
        count = recordCount,
        status = status.name.lowercase(),
    )
