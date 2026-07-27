/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RefreshTokenCookieService(
    @Value("\${jwt.refreshCookieName:fairshare_refresh}") private val cookieName: String,
    @Value("\${jwt.refreshExpirationDays:30}") private val refreshExpirationDays: Long,
) {
    fun readRefreshToken(request: HttpServletRequest): String? =
        request.cookies
            ?.firstOrNull { it.name == cookieName }
            ?.value
            ?.trim()
            ?.ifBlank { null }

    fun writeRefreshToken(
        request: HttpServletRequest,
        response: HttpServletResponse,
        refreshToken: String,
    ) {
        val cookie =
            ResponseCookie
                .from(cookieName, refreshToken)
                .httpOnly(true)
                .secure(isSecureRequest(request))
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ofDays(refreshExpirationDays))
                .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    fun clearRefreshToken(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ) {
        val cookie =
            ResponseCookie
                .from(cookieName, "")
                .httpOnly(true)
                .secure(isSecureRequest(request))
                .sameSite("Strict")
                .path("/")
                .maxAge(Duration.ZERO)
                .build()
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    private fun isSecureRequest(request: HttpServletRequest): Boolean {
        val forwardedProto = request.getHeader("X-Forwarded-Proto")
        return request.isSecure || forwardedProto.equals("https", ignoreCase = true)
    }
}

