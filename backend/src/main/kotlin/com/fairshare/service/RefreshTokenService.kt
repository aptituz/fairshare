/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.model.RefreshToken
import com.fairshare.repo.PersonRepository
import com.fairshare.repo.RefreshTokenRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64

@Service
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val personRepository: PersonRepository,
    @Value("\${jwt.refreshExpirationDays:15}") private val refreshExpirationDays: Long,
) {
    fun issueForPerson(
        personId: Long,
        tokenVersion: Long,
    ): String {
        val rawToken = generateRawToken()
        persistToken(personId, tokenVersion, rawToken)
        return rawToken
    }

    @Transactional
    fun rotate(rawToken: String): RefreshTokenRotationResult? {
        val current = findActiveToken(rawToken) ?: return null
        val person = personRepository.findById(current.personId).orElse(null) ?: return null
        if (current.tokenVersion != person.tokenVersion) {
            current.revokedAt = Instant.now()
            refreshTokenRepository.save(current)
            return null
        }
        current.revokedAt = Instant.now()
        refreshTokenRepository.save(current)

        val newRawToken = generateRawToken()
        persistToken(current.personId, current.tokenVersion, newRawToken)
        return RefreshTokenRotationResult(personId = current.personId, refreshToken = newRawToken)
    }

    fun revoke(rawToken: String) {
        val current = findActiveToken(rawToken) ?: return
        current.revokedAt = Instant.now()
        refreshTokenRepository.save(current)
    }

    @Transactional
    fun revokeAllForPerson(personId: Long) {
        refreshTokenRepository.revokeAllForPerson(personId, Instant.now())
    }

    private fun findActiveToken(rawToken: String): RefreshToken? {
        val tokenHash = hashToken(rawToken)
        val token = refreshTokenRepository.findByTokenHash(tokenHash) ?: return null
        if (token.revokedAt != null) {
            return null
        }
        val maximumExpiry = token.createdAt.plus(refreshExpirationDays, ChronoUnit.DAYS)
        if (token.expiresAt.isBefore(Instant.now()) || maximumExpiry.isBefore(Instant.now())) {
            return null
        }
        return token
    }

    private fun persistToken(
        personId: Long,
        tokenVersion: Long,
        rawToken: String,
    ) {
        val now = Instant.now()
        val entity =
            RefreshToken(
                tokenHash = hashToken(rawToken),
                personId = personId,
                tokenVersion = tokenVersion,
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
