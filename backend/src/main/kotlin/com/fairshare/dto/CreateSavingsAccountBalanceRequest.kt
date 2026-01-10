/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.dto

import java.math.BigDecimal
import java.time.LocalDate

data class CreateSavingsAccountBalanceRequest(
    val balanceDate: LocalDate,
    val balanceAmount: BigDecimal,
)
