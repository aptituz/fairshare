/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.controller

import com.fairshare.dto.CreateSavingsAccountRequest
import com.fairshare.dto.SavingsAccountResponse
import com.fairshare.dto.UpdateSavingsAccountRequest
import com.fairshare.service.SavingsAccountService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/savings-accounts")
@Tag(name = "Savings Accounts", description = "Manage savings accounts.")
class SavingsAccountController(
    private val savingsAccountService: SavingsAccountService,
) {
    @GetMapping
    @Operation(summary = "List savings accounts")
    fun list(): List<SavingsAccountResponse> = savingsAccountService.list()

    @PostMapping
    @Operation(summary = "Create a savings account")
    fun create(
        @RequestBody request: CreateSavingsAccountRequest,
    ): SavingsAccountResponse = savingsAccountService.create(request)

    @PutMapping("/{id}")
    @Operation(summary = "Update a savings account")
    fun update(
        @PathVariable id: Long,
        @RequestBody request: UpdateSavingsAccountRequest,
    ): SavingsAccountResponse = savingsAccountService.update(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a savings account")
    fun delete(
        @PathVariable id: Long,
    ) {
        savingsAccountService.delete(id)
    }
}
