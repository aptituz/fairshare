/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.CreatePersonRequest
import com.fairshare.dto.PersonResponse
import com.fairshare.dto.UpdatePersonRequest
import com.fairshare.exception.BadRequestException
import com.fairshare.exception.ConflictException
import com.fairshare.exception.NotFoundException
import com.fairshare.mapper.toResponse
import com.fairshare.model.Person
import com.fairshare.repo.BudgetItemRepository
import com.fairshare.repo.PersonRepository
import org.springframework.stereotype.Service

@Service
class PersonService(
    private val personRepository: PersonRepository,
    private val budgetItemRepository: BudgetItemRepository
) {
    fun list(): List<PersonResponse> = personRepository.findAll().map { it.toResponse() }

    fun create(request: CreatePersonRequest): PersonResponse {
        val name = request.name.trim()
        if (name.isBlank()) {
            throw BadRequestException("Person name cannot be blank")
        }
        val saved = personRepository.save(Person(name = name))
        return saved.toResponse()
    }

    fun update(
        id: Long,
        request: UpdatePersonRequest,
    ): PersonResponse {
        val person =
            personRepository.findById(id).orElseThrow {
                NotFoundException("Person $id not found")
            }
        val name = request.name.trim()
        if (name.isBlank()) {
            throw BadRequestException("Person name cannot be blank")
        }
        person.name = name
        return personRepository.save(person).toResponse()
    }

    fun delete(id: Long) {
        val person =
            personRepository.findById(id).orElseThrow {
                NotFoundException("Person $id not found")
            }
        if (budgetItemRepository.existsByPersonId(id)) {
            throw ConflictException("Person $id is used by budget items")
        }
        personRepository.delete(person)
    }
}
