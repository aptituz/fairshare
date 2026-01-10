/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.repo

import com.fairshare.model.SavingsAccount
import org.springframework.data.jpa.repository.JpaRepository

interface SavingsAccountRepository : JpaRepository<SavingsAccount, Long>
