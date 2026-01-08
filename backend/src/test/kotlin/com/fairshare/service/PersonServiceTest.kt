/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.CreatePersonRequest
import com.fairshare.exception.BadRequestException
import com.fairshare.exception.ConflictException
import com.fairshare.model.Person
import com.fairshare.repo.BudgetItemRepository
import com.fairshare.repo.PersonRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class PersonServiceTest {

    @Mock
    lateinit var personRepository: PersonRepository

    @Mock
    lateinit var budgetItemRepository: BudgetItemRepository

    @InjectMocks
    lateinit var personService: PersonService

    @Test
    fun `list should return all persons`() {
        // given
        val persons = listOf(
            Person(1, "Person 1"),
            Person(2, "Person 2")
        )
        `when`(personRepository.findAll()).thenReturn(persons)

        // when
        val result = personService.list()

        // then
        assertEquals(2, result.size)
        assertEquals(1, result[0].id)
        assertEquals("Person 1", result[0].name)
        assertEquals(2, result[1].id)
        assertEquals("Person 2", result[1].name)
    }

    @Test
    fun `create should save and return a new person`() {
        // given
        val request = CreatePersonRequest("New Person")
        val person = Person(1, "New Person")
        `when`(personRepository.save(any(Person::class.java))).thenReturn(person)

        // when
        val result = personService.create(request)

        // then
        assertEquals(1, result.id)
        assertEquals("New Person", result.name)
    }

    @Test
    fun `create should throw an exception when name is blank`() {
        // given
        val request = CreatePersonRequest(" ")

        // when / then
        assertThrows(BadRequestException::class.java) {
            personService.create(request)
        }
    }

    @Test
    fun `delete should remove a person when they are not used by budget items`() {
        // given
        val person = Person(1, "Test Person")
        `when`(personRepository.findById(1)).thenReturn(java.util.Optional.of(person))
        `when`(budgetItemRepository.existsByPersonId(1)).thenReturn(false)

        // when
        personService.delete(1)

        // then
        // No exception should be thrown
    }

    @Test
    fun `delete should throw an exception when person is used by budget items`() {
        // given
        val person = Person(1, "Test Person")
        `when`(personRepository.findById(1)).thenReturn(java.util.Optional.of(person))
        `when`(budgetItemRepository.existsByPersonId(1)).thenReturn(true)

        // when / then
        assertThrows(ConflictException::class.java) {
            personService.delete(1)
        }
    }
}
