/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.security.MessageDigest
import java.util.Base64

class PasswordServiceTest {
    private val passwordService = PasswordService(BCryptPasswordEncoder())

    @Test
    fun `bcrypt hashes should verify without upgrade`() {
        val hash = passwordService.encode("secure-password")

        val result = passwordService.verify("secure-password", hash, null)

        assertTrue(result.matches)
        assertFalse(result.upgradedHash != null)
    }

    @Test
    fun `legacy hashes should verify and request upgrade`() {
        val salt = Base64.getEncoder().encodeToString(ByteArray(16) { 1 })
        val legacyHash = legacyHash("legacy-password", salt)

        val result = passwordService.verify("legacy-password", legacyHash, salt)

        assertTrue(result.matches)
        assertNotNull(result.upgradedHash)
    }

    @Test
    fun `legacy hashes should reject mismatched password`() {
        val salt = Base64.getEncoder().encodeToString(ByteArray(16) { 2 })
        val legacyHash = legacyHash("legacy-password", salt)

        val result = passwordService.verify("wrong-password", legacyHash, salt)

        assertFalse(result.matches)
    }

    private fun legacyHash(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(Base64.getDecoder().decode(salt))
        val hashed = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashed.joinToString("") { "%02x".format(it) }
    }
}
