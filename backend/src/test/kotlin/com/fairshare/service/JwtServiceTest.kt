/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.time.Duration

class JwtServiceTest {
    private val secret = "test-secret-test-secret-test-secret-test-secret"
    private val jwtService = JwtService(secret, 15)

    @Test
    fun `generated access token should contain version and expire after fifteen minutes`() {
        val token = jwtService.generateToken("alex", 7)
        val claims = jwtService.parseToken(token)
        val payload =
            Jwts
                .parser()
                .verifyWith(Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .payload

        assertEquals(JwtTokenClaims(username = "alex", tokenVersion = 7), claims)
        assertEquals(Duration.ofMinutes(15).seconds, (payload.expiration.time - payload.issuedAt.time) / 1000)
    }
}
