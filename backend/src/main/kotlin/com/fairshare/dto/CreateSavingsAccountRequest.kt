/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.dto

data class CreateSavingsAccountRequest(
    val name: String,
    val ownerId: Long?,
    val startDate: java.time.LocalDate,
    val endDate: java.time.LocalDate?,
)
