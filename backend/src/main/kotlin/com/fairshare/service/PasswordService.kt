/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.Base64

@Service
class PasswordService(
    private val passwordEncoder: PasswordEncoder,
) {
    fun encode(password: String): String = passwordEncoder.encode(password)

    fun verify(
        password: String,
        storedHash: String,
        storedSalt: String?,
    ): PasswordVerificationResult {
        return if (isBcryptHash(storedHash)) {
            PasswordVerificationResult(matches = passwordEncoder.matches(password, storedHash))
        } else {
            val legacySalt = storedSalt ?: return PasswordVerificationResult(matches = false)
            val legacyHash = hashLegacyPassword(password, legacySalt)
            if (legacyHash == storedHash) {
                PasswordVerificationResult(
                    matches = true,
                    upgradedHash = passwordEncoder.encode(password),
                )
            } else {
                PasswordVerificationResult(matches = false)
            }
        }
    }

    private fun isBcryptHash(hash: String): Boolean = hash.startsWith("\$2a\$") || hash.startsWith("\$2b\$") || hash.startsWith("\$2y\$")

    private fun hashLegacyPassword(
        password: String,
        salt: String,
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(Base64.getDecoder().decode(salt))
        val hashed = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashed.joinToString("") { "%02x".format(it) }
    }
}
