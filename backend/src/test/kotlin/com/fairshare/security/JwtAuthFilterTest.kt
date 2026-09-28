/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.security

import com.fairshare.model.Person
import com.fairshare.repo.PersonRepository
import com.fairshare.service.JwtService
import com.fairshare.service.JwtTokenClaims
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder

class JwtAuthFilterTest {
    private val jwtService = mock(JwtService::class.java)
    private val personRepository = mock(PersonRepository::class.java)
    private val filter = JwtAuthFilter(jwtService, personRepository)

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `valid versioned token authenticates request`() {
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer valid-token")
        val person = Person(id = 1L, name = "Alex", username = "alex", tokenVersion = 3)
        `when`(jwtService.parseToken("valid-token")).thenReturn(JwtTokenClaims("alex", 3))
        `when`(personRepository.findByUsername("alex")).thenReturn(person)

        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        assertEquals("alex", SecurityContextHolder.getContext().authentication?.name)
    }

    @Test
    fun `token with stale version remains unauthenticated`() {
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer stale-token")
        val person = Person(id = 1L, name = "Alex", username = "alex", tokenVersion = 4)
        `when`(jwtService.parseToken("stale-token")).thenReturn(JwtTokenClaims("alex", 3))
        `when`(personRepository.findByUsername("alex")).thenReturn(person)

        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        assertNull(SecurityContextHolder.getContext().authentication)
    }
}
