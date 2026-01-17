/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.mapper

import com.fairshare.dto.SavingsAccountBalanceResponse
import com.fairshare.model.SavingsAccountBalance

fun SavingsAccountBalance.toResponse(): SavingsAccountBalanceResponse =
    SavingsAccountBalanceResponse(
        id = id,
        savingsAccountId =
            savingsAccount.id
                ?: throw IllegalStateException("Savings account must be persisted before mapping"),
        balanceDate = balanceDate,
        balanceAmount = balanceAmount,
    )
