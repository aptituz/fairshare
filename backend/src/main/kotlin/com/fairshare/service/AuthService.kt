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
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

@Service
class AuthService(
    private val personRepository: PersonRepository,
    private val jwtService: JwtService,
) {
    private val random = SecureRandom()

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
        val salt = generateSalt()
        val hash = hashPassword(password, salt)
        person.passwordSalt = salt
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
        val salt = person.passwordSalt ?: throw BadRequestException("Password not set")
        val hash = person.passwordHash ?: throw BadRequestException("Password not set")
        val candidate = hashPassword(password, salt)
        if (candidate != hash) {
            throw BadRequestException("Invalid credentials")
        }
        return AuthResponse(token = jwtService.generateToken(person.username))
    }

    fun currentUser(): AuthUserResponse {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: throw BadRequestException("Not authenticated")
        val person =
            personRepository.findByUsername(username)
                ?: throw NotFoundException("User $username not found")
        return AuthUserResponse(
            username = person.username,
            name = person.name,
        )
    }

    fun changePassword(request: ChangePasswordRequest) {
        val username = SecurityContextHolder.getContext().authentication?.name
            ?: throw BadRequestException("Not authenticated")
        val person =
            personRepository.findByUsername(username)
                ?: throw NotFoundException("User $username not found")
        val currentSalt = person.passwordSalt ?: throw BadRequestException("Password not set")
        val currentHash = person.passwordHash ?: throw BadRequestException("Password not set")
        val candidate = hashPassword(request.currentPassword, currentSalt)
        if (candidate != currentHash) {
            throw BadRequestException("Current password is incorrect")
        }
        if (request.newPassword.isBlank()) {
            throw BadRequestException("New password cannot be blank")
        }
        val newSalt = generateSalt()
        val newHash = hashPassword(request.newPassword, newSalt)
        person.passwordSalt = newSalt
        person.passwordHash = newHash
        personRepository.save(person)
    }

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    private fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(Base64.getDecoder().decode(salt))
        val hashed = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashed.joinToString("") { "%02x".format(it) }
    }
}
