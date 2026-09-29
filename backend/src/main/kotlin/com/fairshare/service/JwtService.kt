/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

@Service
class JwtService(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expirationMinutes}") private val expirationMinutes: Long,
) {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))
    private val log = LoggerFactory.getLogger(javaClass)

    fun generateToken(
        username: String,
        tokenVersion: Long,
    ): String {
        val now = Instant.now()
        val expiry = now.plus(expirationMinutes, ChronoUnit.MINUTES)
        return Jwts
            .builder()
            .subject(username)
            .claim("ver", tokenVersion)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(key)
            .compact()
    }

    fun parseToken(token: String): JwtTokenClaims? =
        try {
            Jwts
                .parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
                .let { claims ->
                    val version = claims["ver", Number::class.java]?.toLong()
                    if (version == null) {
                        log.debug("JWT validation failed requestId={} reason=missing_token_version", MDC.get("requestId"))
                        return null
                    }
                    JwtTokenClaims(username = claims.subject, tokenVersion = version)
                }
        } catch (ex: Exception) {
            // Exception messages can contain claims; only record the exception type.
            log.debug("JWT validation failed requestId={} reason={}", MDC.get("requestId"), ex.javaClass.simpleName)
            null
        }
}
