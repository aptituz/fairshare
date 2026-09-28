/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.security

import com.fairshare.controller.AuthController
import com.fairshare.dto.AuthUserResponse
import com.fairshare.model.Person
import com.fairshare.repo.PersonRepository
import com.fairshare.service.AuthService
import com.fairshare.service.JwtService
import com.fairshare.service.JwtTokenClaims
import com.fairshare.service.RefreshTokenCookieService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(AuthController::class)
@AutoConfigureMockMvc
@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration::class, SecurityFilterAutoConfiguration::class)
@Import(SecurityConfig::class, JwtAuthFilter::class)
class JwtAuthenticationIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var authService: AuthService

    @MockitoBean
    private lateinit var refreshTokenCookieService: RefreshTokenCookieService

    @MockitoBean
    private lateinit var jwtService: JwtService

    @MockitoBean
    private lateinit var personRepository: PersonRepository

    @Test
    fun `bearer token authenticates protected endpoint`() {
        val person = Person(id = 1L, name = "Alex", username = "alex", tokenVersion = 3)
        `when`(jwtService.parseToken("valid-token")).thenReturn(JwtTokenClaims("alex", 3))
        `when`(personRepository.findByUsername("alex")).thenReturn(person)
        `when`(authService.currentUser()).thenReturn(AuthUserResponse(username = "alex", name = "Alex"))

        mockMvc
            .get("/api/auth/me") {
                header("Authorization", "Bearer valid-token")
            }.andExpect {
                status { isOk() }
                jsonPath("$.username") { value("alex") }
            }
    }
}
