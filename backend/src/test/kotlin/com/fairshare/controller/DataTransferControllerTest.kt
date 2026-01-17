/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.controller

import com.fairshare.dto.BudgetItemExport
import com.fairshare.dto.CategoryExport
import com.fairshare.dto.DataExportPayload
import com.fairshare.dto.PersonExport
import com.fairshare.model.BudgetItemType
import com.fairshare.model.Frequency
import com.fairshare.security.JwtAuthFilter
import com.fairshare.security.SecurityConfig
import com.fairshare.service.DataTransferService
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.math.BigDecimal
import java.time.LocalDate

@WebMvcTest(DataTransferController::class)
@AutoConfigureMockMvc
@ImportAutoConfiguration(SecurityAutoConfiguration::class, SecurityFilterAutoConfiguration::class)
@Import(SecurityConfig::class)
class DataTransferControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {
    @MockBean
    private lateinit var dataTransferService: DataTransferService

    @MockBean
    private lateinit var jwtAuthFilter: JwtAuthFilter

    @BeforeEach
    fun setupFilters() {
        doAnswer { invocation ->
            val request = invocation.getArgument<HttpServletRequest>(0)
            val response = invocation.getArgument<HttpServletResponse>(1)
            val chain = invocation.getArgument<FilterChain>(2)
            chain.doFilter(request, response)
            null
        }.`when`(jwtAuthFilter).doFilter(any(), any(), any())
    }

    @Test
    @WithMockUser(username = "alex")
    fun `export should return payload`() {
        val payload =
            DataExportPayload(
                persons = listOf(PersonExport(id = 1, name = "Alex", username = "alex")),
                categories = listOf(CategoryExport(id = 2, name = "Gehalt", type = BudgetItemType.INCOME, rank = 1)),
                budgetItems =
                    listOf(
                        BudgetItemExport(
                            id = 3,
                            name = "Miete",
                            amount = BigDecimal("1000"),
                            type = BudgetItemType.EXPENSE,
                            frequency = Frequency.MONTHLY,
                            planned = true,
                            categoryCorrection = false,
                            startDate = LocalDate.of(2025, 1, 1),
                            endDate = null,
                            dueDate = null,
                            categoryId = 2,
                            personId = 1,
                            previousBudgetItemId = null,
                            rootBudgetItemId = 3,
                        ),
                    ),
                budgetItemSuspensions = emptyList(),
                savingsAccounts = emptyList(),
                savingsAccountBalances = emptyList(),
            )

        `when`(dataTransferService.exportData()).thenReturn(payload)

        mockMvc
            .get("/api/data/export")
            .andExpect {
                status { isOk() }
                content { contentType(MediaType.APPLICATION_JSON) }
                jsonPath("$.version") { value(1) }
                jsonPath("$.persons[0].name") { value("Alex") }
                jsonPath("$.categories[0].name") { value("Gehalt") }
                jsonPath("$.budgetItems[0].name") { value("Miete") }
            }
    }

    @Test
    fun `export should be rejected when unauthenticated`() {
        mockMvc
            .get("/api/data/export")
            .andExpect {
                status { isForbidden() }
            }
    }

    @Test
    @WithMockUser(username = "alex")
    fun `import should accept payload`() {
        val payload =
            DataExportPayload(
                persons = listOf(PersonExport(id = 1, name = "Alex", username = "alex")),
                categories = emptyList(),
                budgetItems = emptyList(),
                budgetItemSuspensions = emptyList(),
                savingsAccounts = emptyList(),
                savingsAccountBalances = emptyList(),
            )

        mockMvc
            .post("/api/data/import") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(payload)
            }.andExpect {
                status { isNoContent() }
            }

        verify(dataTransferService).importData(payload)
    }

    @Test
    fun `import should be rejected when unauthenticated`() {
        val payload =
            DataExportPayload(
                persons = emptyList(),
                categories = emptyList(),
                budgetItems = emptyList(),
                budgetItemSuspensions = emptyList(),
                savingsAccounts = emptyList(),
                savingsAccountBalances = emptyList(),
            )

        mockMvc
            .post("/api/data/import") {
                contentType = MediaType.APPLICATION_JSON
                content = objectMapper.writeValueAsString(payload)
            }.andExpect {
                status { isForbidden() }
            }
    }
}
