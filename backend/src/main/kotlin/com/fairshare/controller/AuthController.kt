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
import com.fairshare.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authenticate users and setup initial password.")
class AuthController(
    private val authService: AuthService,
) {
    @GetMapping("/status")
    @Operation(summary = "Check if authentication is configured")
    fun status(): AuthStatusResponse = authService.status()

    @PostMapping("/setup")
    @Operation(summary = "Set initial password when no password is configured")
    fun setup(
        @RequestBody request: SetupPasswordRequest,
    ): AuthResponse = authService.setup(request)

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive a JWT")
    fun login(
        @RequestBody request: AuthRequest,
    ): AuthResponse = authService.login(request)

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
