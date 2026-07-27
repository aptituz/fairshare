/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.AuthRequest
import com.fairshare.dto.AuthStatusResponse
import com.fairshare.dto.AuthUserResponse
import com.fairshare.dto.ChangePasswordRequest
import com.fairshare.dto.SetupPasswordRequest
import com.fairshare.exception.BadRequestException
import com.fairshare.exception.NotFoundException
import com.fairshare.exception.UnauthorizedException
import com.fairshare.repo.PersonRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val personRepository: PersonRepository,
    private val jwtService: JwtService,
    private val passwordService: PasswordService,
    private val refreshTokenService: RefreshTokenService,
) {
    fun status(): AuthStatusResponse = AuthStatusResponse(ready = personRepository.existsByPasswordHashIsNotNull())

    fun setup(request: SetupPasswordRequest): AuthSessionTokens {
        if (personRepository.existsByPasswordHashIsNotNull()) {
            throw BadRequestException("Setup already completed")
        }
        val username = request.username.trim()
        val password = request.password
        if (username.isBlank() || password.isBlank()) {
            throw BadRequestException("Username and password are required")
        }
        val person =
            personRepository.findByUsername(username)
                ?: throw NotFoundException("User $username not found")
        val hash = passwordService.encode(password)
        person.passwordSalt = null
        person.passwordHash = hash
        personRepository.save(person)
        return issueSessionTokens(
            person.id ?: throw BadRequestException("User id missing"),
            person.username,
        )
    }

    fun login(request: AuthRequest): AuthSessionTokens {
        val username = request.username.trim()
        val password = request.password
        if (username.isBlank() || password.isBlank()) {
            throw BadRequestException("Username and password are required")
        }
        val person =
            personRepository.findByUsername(username)
                ?: throw NotFoundException("User $username not found")
        val hash = person.passwordHash ?: throw BadRequestException("Password not set")
        val verification = passwordService.verify(password, hash, person.passwordSalt)
        if (!verification.matches) {
            throw BadRequestException("Invalid credentials")
        }
        if (verification.upgradedHash != null) {
            person.passwordHash = verification.upgradedHash
            person.passwordSalt = null
            personRepository.save(person)
        }
        return issueSessionTokens(
            person.id ?: throw BadRequestException("User id missing"),
            person.username,
        )
    }

    fun refresh(refreshToken: String): AuthSessionTokens {
        val rotation =
            refreshTokenService.rotate(refreshToken)
                ?: throw UnauthorizedException("Invalid refresh token")
        val person =
            personRepository.findById(rotation.personId).orElseThrow {
                NotFoundException("User ${rotation.personId} not found")
            }
        return AuthSessionTokens(
            accessToken = jwtService.generateToken(person.username),
            refreshToken = rotation.refreshToken,
        )
    }

    fun logout(refreshToken: String?) {
        if (refreshToken.isNullOrBlank()) {
            return
        }
        refreshTokenService.revoke(refreshToken)
    }

    fun currentUser(): AuthUserResponse {
        val username = resolveAuthenticatedUsername()
        val person =
            personRepository.findByUsername(username)
                ?: throw NotFoundException("User $username not found")
        return AuthUserResponse(
            username = person.username,
            name = person.name,
        )
    }

    fun changePassword(request: ChangePasswordRequest) {
        val username = resolveAuthenticatedUsername()
        val person =
            personRepository.findByUsername(username)
                ?: throw NotFoundException("User $username not found")
        val currentHash = person.passwordHash ?: throw BadRequestException("Password not set")
        val verification = passwordService.verify(request.currentPassword, currentHash, person.passwordSalt)
        if (!verification.matches) {
            throw BadRequestException("Current password is incorrect")
        }
        if (request.newPassword.isBlank()) {
            throw BadRequestException("New password cannot be blank")
        }
        val newHash = passwordService.encode(request.newPassword)
        person.passwordSalt = null
        person.passwordHash = newHash
        personRepository.save(person)
    }

    private fun resolveAuthenticatedUsername(): String {
        val username = SecurityContextHolder.getContext().authentication?.name
        if (username.isNullOrBlank() || username == "anonymousUser") {
            throw BadRequestException("Not authenticated")
        }
        return username
    }

    fun setPasswordForPerson(
        personId: Long,
        newPassword: String,
    ) {
        if (newPassword.isBlank()) {
            throw BadRequestException("New password cannot be blank")
        }
        val person =
            personRepository.findById(personId).orElseThrow {
                NotFoundException("Person $personId not found")
            }
        val newHash = passwordService.encode(newPassword)
        person.passwordSalt = null
        person.passwordHash = newHash
        personRepository.save(person)
    }

    private fun issueSessionTokens(
        personId: Long,
        username: String,
    ): AuthSessionTokens {
        val accessToken = jwtService.generateToken(username)
        val refreshToken = refreshTokenService.issueForPerson(personId)
        return AuthSessionTokens(accessToken = accessToken, refreshToken = refreshToken)
    }
}
