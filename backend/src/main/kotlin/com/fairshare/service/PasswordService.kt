/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
@Service
class PasswordService(
    private val passwordEncoder: PasswordEncoder,
) {
    fun encode(password: String): String = passwordEncoder.encode(password)

    fun matches(password: String, storedHash: String): Boolean = passwordEncoder.matches(password, storedHash)
}
