/*
 * Copyright (C) 2026 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.security

import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.MDC
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

@ExtendWith(OutputCaptureExtension::class)
class RequestDiagnosticsFilterTest {
    private val filter = RequestDiagnosticsFilter("custom_refresh")

    @Test
    fun `failure logs include correlation and cookie presence without credentials`(output: CapturedOutput) {
        val request = MockHttpServletRequest("GET", "/fairshare/api/savings-accounts")
        request.queryString = "secret=private-query"
        request.addHeader("Authorization", "Bearer private-token")
        request.addHeader("X-Request-ID", "untrusted-id")
        request.setCookies(Cookie("custom_refresh", "private-cookie"))
        val response = MockHttpServletResponse()
        MDC.put("requestId", "outer")
        try {
            filter.doFilter(request, response) { _, _ ->
                assertEquals(response.getHeader("X-Request-ID"), MDC.get("requestId"))
                response.status = 401
            }
            val id = response.getHeader("X-Request-ID")
            assertNotNull(id)
            assertTrue(output.out.contains("requestId=$id"))
            assertTrue(output.out.contains("path=/fairshare/api/savings-accounts status=401"))
            assertTrue(output.out.contains("refreshCookiePresent=true"))
            for (secret in listOf("private-token", "private-query", "private-cookie", "untrusted-id")) {
                assertFalse(output.out.contains(secret))
            }
            assertEquals("outer", MDC.get("requestId"))
        } finally {
            MDC.remove("requestId")
        }
    }

    @Test
    fun `exceptions propagate and clean up MDC`(output: CapturedOutput) {
        val exception = IllegalStateException("private-detail")
        assertEquals(
            exception,
            assertThrows(IllegalStateException::class.java) {
                filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse()) { _, _ -> throw exception }
            },
        )
        assertEquals(null, MDC.get("requestId"))
        assertTrue(output.out.contains("status=500"))
        assertFalse(output.out.contains("private-detail"))
    }
}
