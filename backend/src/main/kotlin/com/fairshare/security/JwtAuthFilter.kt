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
        request.setAttribute("fairshare.authOutcome", if (header.isEmpty()) "missing_header" else "unsupported_scheme")
        if (header.startsWith("Bearer ")) {
            val token = header.removePrefix("Bearer ").trim()
            val claims = jwtService.parseToken(token)
            request.setAttribute("fairshare.authOutcome", "invalid_token")
            if (claims != null && SecurityContextHolder.getContext().authentication == null) {
                val person = personRepository.findByUsername(claims.username)
                request.setAttribute(
                    "fairshare.authOutcome",
                    if (person == null) "unknown_user" else "stale_token_version",
                )
                if (person != null && person.tokenVersion == claims.tokenVersion) {
                    val auth = UsernamePasswordAuthenticationToken(claims.username, null, emptyList())
                    val context = SecurityContextHolder.createEmptyContext()
                    context.authentication = auth
                    SecurityContextHolder.setContext(context)
                    request.setAttribute("fairshare.authOutcome", "authenticated")
                }
            }
        }
        if (SecurityContextHolder.getContext().authentication?.isAuthenticated == true) {
            request.setAttribute("fairshare.authOutcome", "authenticated")
        }
        filterChain.doFilter(request, response)
    }
}
