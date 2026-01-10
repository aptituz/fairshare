/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.CreateSavingsAccountRequest
import com.fairshare.dto.SavingsAccountResponse
import com.fairshare.dto.UpdateSavingsAccountRequest
import com.fairshare.exception.BadRequestException
import com.fairshare.exception.NotFoundException
import com.fairshare.mapper.toResponse
import com.fairshare.model.SavingsAccount
import com.fairshare.repo.PersonRepository
import com.fairshare.repo.SavingsAccountRepository
import org.springframework.stereotype.Service

@Service
class SavingsAccountService(
    private val savingsAccountRepository: SavingsAccountRepository,
    private val personRepository: PersonRepository,
) {
    fun list(): List<SavingsAccountResponse> =
        savingsAccountRepository.findAll().map { it.toResponse() }

    fun create(request: CreateSavingsAccountRequest): SavingsAccountResponse {
        val name = request.name.trim()
        if (name.isBlank()) {
            throw BadRequestException("Savings account name cannot be blank")
        }
        val owner =
            request.ownerId?.let { ownerId ->
                personRepository.findById(ownerId).orElseThrow {
                    NotFoundException("Person $ownerId not found")
                }
            }
        val saved = savingsAccountRepository.save(SavingsAccount(name = name, owner = owner))
        return saved.toResponse()
    }

    fun update(
        id: Long,
        request: UpdateSavingsAccountRequest,
    ): SavingsAccountResponse {
        val account =
            savingsAccountRepository.findById(id).orElseThrow {
                NotFoundException("Savings account $id not found")
            }
        val name = request.name.trim()
        if (name.isBlank()) {
            throw BadRequestException("Savings account name cannot be blank")
        }
        val owner =
            request.ownerId?.let { ownerId ->
                personRepository.findById(ownerId).orElseThrow {
                    NotFoundException("Person $ownerId not found")
                }
            }
        account.name = name
        account.owner = owner
        return savingsAccountRepository.save(account).toResponse()
    }

    fun delete(id: Long) {
        val account =
            savingsAccountRepository.findById(id).orElseThrow {
                NotFoundException("Savings account $id not found")
            }
        savingsAccountRepository.delete(account)
    }
}
