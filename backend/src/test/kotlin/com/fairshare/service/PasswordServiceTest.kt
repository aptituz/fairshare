/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class PasswordServiceTest {
    private val passwordService = PasswordService(BCryptPasswordEncoder())

    @Test
    fun `bcrypt hashes should verify`() {
        val hash = passwordService.encode("secure-password")

        val result = passwordService.matches("secure-password", hash)

        assertTrue(result)
    }

    @Test
    fun `bcrypt hashes should reject mismatched password`() {
        val hash = passwordService.encode("secure-password")
        val result = passwordService.matches("wrong-password", hash)

        assertFalse(result)
    }
}
