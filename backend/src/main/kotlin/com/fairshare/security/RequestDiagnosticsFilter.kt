/*
 * Copyright (C) 2026 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestDiagnosticsFilter(
    @Value("\${jwt.refreshCookieName:fairshare_refresh}") private val refreshCookieName: String,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        // Do not trust client-supplied IDs as log content.
        val requestId = UUID.randomUUID().toString()
        val previousId = MDC.get("requestId")
        MDC.put("requestId", requestId)
        response.setHeader("X-Request-ID", requestId)
        val started = System.nanoTime()
        var failed = false
        try {
            filterChain.doFilter(request, response)
        } catch (ex: Exception) {
            failed = true
            throw ex
        } finally {
            val status = if (failed) 500 else response.status
            val message = "HTTP requestId={} method={} path={} status={} durationMs={} auth={} refreshCookiePresent={}"
            val arguments =
                arrayOf(
                    requestId,
                    safe(request.method),
                    safe(request.requestURI),
                    status,
                    (System.nanoTime() - started) / 1_000_000,
                    request.getAttribute("fairshare.authOutcome") ?: "not_evaluated",
                    request.cookies?.any { it.name == refreshCookieName && it.value.isNotBlank() } == true,
                )
            if (status >= 400) {
                log.warn(message, *arguments)
            } else {
                log.debug(message, *arguments)
            }
            if (previousId == null) MDC.remove("requestId") else MDC.put("requestId", previousId)
        }
    }

    private fun safe(value: String): String = value.replace(Regex("[\\p{Cntrl}]"), "_").take(512)
}
