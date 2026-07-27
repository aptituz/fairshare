/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.model.RefreshToken
import com.fairshare.repo.RefreshTokenRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    @Value("\${jwt.refreshExpirationDays:30}") private val refreshExpirationDays: Long,
) {
    fun issueForPerson(personId: Long): String {
        val rawToken = generateRawToken()
        persistToken(personId, rawToken)
        return rawToken
    }

    fun rotate(rawToken: String): RefreshTokenRotationResult? {
        val current = findActiveToken(rawToken) ?: return null
        current.revokedAt = Instant.now()
        refreshTokenRepository.save(current)

        val newRawToken = generateRawToken()
        persistToken(current.personId, newRawToken)
        return RefreshTokenRotationResult(personId = current.personId, refreshToken = newRawToken)
    }

    fun revoke(rawToken: String) {
        val current = findActiveToken(rawToken) ?: return
        current.revokedAt = Instant.now()
        refreshTokenRepository.save(current)
    }

    private fun findActiveToken(rawToken: String): RefreshToken? {
        val tokenHash = hashToken(rawToken)
        val token = refreshTokenRepository.findByTokenHash(tokenHash) ?: return null
        if (token.revokedAt != null) {
            return null
        }
        if (token.expiresAt.isBefore(Instant.now())) {
            return null
        }
        return token
    }

    private fun persistToken(
        personId: Long,
        rawToken: String,
    ) {
        val now = Instant.now()
        val entity =
            RefreshToken(
                tokenHash = hashToken(rawToken),
                personId = personId,
                expiresAt = now.plus(refreshExpirationDays, ChronoUnit.DAYS),
                createdAt = now,
            )
        refreshTokenRepository.save(entity)
    }

    private fun generateRawToken(): String {
        val bytes = ByteArray(32)
        java.security.SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

