/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.AuthRequest
import com.fairshare.dto.AuthResponse
import com.fairshare.dto.AuthStatusResponse
import com.fairshare.dto.AuthUserResponse
import com.fairshare.dto.ChangePasswordRequest
import com.fairshare.dto.SetupPasswordRequest
import com.fairshare.exception.BadRequestException
import com.fairshare.exception.NotFoundException
import com.fairshare.repo.PersonRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val personRepository: PersonRepository,
    private val jwtService: JwtService,
    private val passwordService: PasswordService,
) {
    fun status(): AuthStatusResponse = AuthStatusResponse(ready = personRepository.existsByPasswordHashIsNotNull())

    fun setup(request: SetupPasswordRequest): AuthResponse {
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
        return AuthResponse(token = jwtService.generateToken(person.username))
    }

    fun login(request: AuthRequest): AuthResponse {
        val username = request.username.trim()
        val password = request.password
        if (username.isBlank() || password.isBlank()) {
            throw BadRequestException("Username and password are required")
        }
        val person =
            personRepository.findByUsername(username)
                ?: throw NotFoundException("User $username not found")
        val hash = person.passwordHash ?: throw BadRequestException("Password not set")
        if (!passwordService.matches(password, hash)) {
            throw BadRequestException("Invalid credentials")
        }
        return AuthResponse(token = jwtService.generateToken(person.username))
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
        if (!passwordService.matches(request.currentPassword, currentHash)) {
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
}
