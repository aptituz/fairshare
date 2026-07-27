/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.AuthRequest
import com.fairshare.exception.UnauthorizedException
import com.fairshare.model.Person
import com.fairshare.repo.PersonRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class AuthServiceTest {
    @Mock
    lateinit var personRepository: PersonRepository

    @Mock
    lateinit var jwtService: JwtService

    @Mock
    lateinit var passwordService: PasswordService

    @Mock
    lateinit var refreshTokenService: RefreshTokenService

    @InjectMocks
    lateinit var authService: AuthService

    @Test
    fun `login should return access and refresh tokens`() {
        val person = Person(id = 1L, name = "Person 1", username = "person1", passwordHash = "hash")
        `when`(personRepository.findByUsername("person1")).thenReturn(person)
        `when`(passwordService.verify("secret", "hash", null)).thenReturn(PasswordVerificationResult(matches = true))
        `when`(jwtService.generateToken("person1")).thenReturn("access-token")
        `when`(refreshTokenService.issueForPerson(1L)).thenReturn("refresh-token")

        val result = authService.login(AuthRequest(username = "person1", password = "secret"))

        assertEquals("access-token", result.accessToken)
        assertEquals("refresh-token", result.refreshToken)
        verify(refreshTokenService).issueForPerson(1L)
    }

    @Test
    fun `refresh should rotate refresh token and return a new access token`() {
        val person = Person(id = 2L, name = "Person 2", username = "person2", passwordHash = "hash")
        val rotation = RefreshTokenRotationResult(personId = 2L, refreshToken = "new-refresh")
        `when`(refreshTokenService.rotate("old-refresh")).thenReturn(rotation)
        `when`(personRepository.findById(2L)).thenReturn(Optional.of(person))
        `when`(jwtService.generateToken("person2")).thenReturn("new-access")

        val result = authService.refresh("old-refresh")

        assertEquals("new-access", result.accessToken)
        assertEquals("new-refresh", result.refreshToken)
    }

    @Test
    fun `refresh should fail when refresh token is invalid`() {
        `when`(refreshTokenService.rotate("bad-token")).thenReturn(null)

        assertThrows(UnauthorizedException::class.java) {
            authService.refresh("bad-token")
        }
    }
}

