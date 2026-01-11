/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.security

import com.fairshare.repo.PersonRepository
import com.fairshare.service.JwtService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
    private val personRepository: PersonRepository,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val header = request.getHeader("Authorization") ?: ""
        if (header.startsWith("Bearer ")) {
            val token = header.removePrefix("Bearer ").trim()
            val username = jwtService.parseUsername(token)
            if (username != null && SecurityContextHolder.getContext().authentication == null) {
                val person = personRepository.findByUsername(username)
                if (person != null) {
                    val auth = UsernamePasswordAuthenticationToken(username, null, emptyList())
                    SecurityContextHolder.getContext().authentication = auth
                }
            }
        }
        filterChain.doFilter(request, response)
    }
}
