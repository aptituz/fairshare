/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

data class RefreshTokenRotationResult(
    val personId: Long,
    val refreshToken: String,
)

