/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.mapper

import com.fairshare.dto.CategoryResponse
import com.fairshare.model.Category

fun Category.toResponse(): CategoryResponse =
    CategoryResponse(
        id = id,
        name = name,
        type = type,
        rank = rank,
    )
