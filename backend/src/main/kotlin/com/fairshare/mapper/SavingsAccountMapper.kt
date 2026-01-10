/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.mapper

import com.fairshare.dto.SavingsAccountResponse
import com.fairshare.model.SavingsAccount

fun SavingsAccount.toResponse(): SavingsAccountResponse =
    SavingsAccountResponse(
        id = id,
        name = name,
        owner = owner?.toResponse(),
    )
