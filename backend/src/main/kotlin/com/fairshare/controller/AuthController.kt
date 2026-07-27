/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.controller

import com.fairshare.dto.AuthRequest
import com.fairshare.dto.AuthResponse
import com.fairshare.dto.AuthStatusResponse
import com.fairshare.dto.AuthUserResponse
import com.fairshare.dto.ChangePasswordRequest
import com.fairshare.dto.SetupPasswordRequest
import com.fairshare.exception.UnauthorizedException
import com.fairshare.service.AuthService
import com.fairshare.service.RefreshTokenCookieService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authenticate users and setup initial password.")
class AuthController(
    private val authService: AuthService,
    private val refreshTokenCookieService: RefreshTokenCookieService,
) {
    @GetMapping("/status")
    @Operation(summary = "Check if authentication is configured")
    fun status(): AuthStatusResponse = authService.status()

    @PostMapping("/setup")
    @Operation(summary = "Set initial password when no password is configured")
    fun setup(
        @RequestBody request: SetupPasswordRequest,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): AuthResponse {
        val session = authService.setup(request)
        refreshTokenCookieService.writeRefreshToken(httpRequest, httpResponse, session.refreshToken)
        return AuthResponse(token = session.accessToken)
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive a JWT")
    fun login(
        @RequestBody request: AuthRequest,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): AuthResponse {
        val session = authService.login(request)
        refreshTokenCookieService.writeRefreshToken(httpRequest, httpResponse, session.refreshToken)
        return AuthResponse(token = session.accessToken)
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token cookie and receive a new JWT")
    fun refresh(
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): AuthResponse {
        val refreshToken =
            refreshTokenCookieService.readRefreshToken(httpRequest)
                ?: throw UnauthorizedException("Refresh token missing")
        val session = authService.refresh(refreshToken)
        refreshTokenCookieService.writeRefreshToken(httpRequest, httpResponse, session.refreshToken)
        return AuthResponse(token = session.accessToken)
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Log out the current device session")
    fun logout(
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ) {
        val refreshToken = refreshTokenCookieService.readRefreshToken(httpRequest)
        authService.logout(refreshToken)
        refreshTokenCookieService.clearRefreshToken(httpRequest, httpResponse)
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current authenticated user")
    fun me(): AuthUserResponse = authService.currentUser()

    @PostMapping("/change-password")
    @Operation(summary = "Change the current user's password")
    fun changePassword(
        @RequestBody request: ChangePasswordRequest,
    ) {
        authService.changePassword(request)
    }
}
