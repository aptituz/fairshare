/*
 * Copyright (C) 2026 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.model.Person
import com.fairshare.model.SavingsAccount
import com.fairshare.repo.PersonRepository
import com.fairshare.repo.SavingsAccountRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:savings-account-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.liquibase.enabled=false",
    ],
)
class SavingsAccountServiceIntegrationTest(
    @Autowired private val service: SavingsAccountService,
    @Autowired private val accountRepository: SavingsAccountRepository,
    @Autowired private val personRepository: PersonRepository,
) {
    @Test
    fun `list maps owners without a caller transaction or open session in view`() {
        // Repository saves finish before list, so the owner must be loaded in a new session.
        val owner = personRepository.save(Person(name = "Alex", username = "alex"))
        val owned = accountRepository.save(SavingsAccount(name = "Personal", owner = owner))
        val shared = accountRepository.save(SavingsAccount(name = "Shared"))

        try {
            val responses = service.list().associateBy { it.id }

            assertEquals("Personal", responses.getValue(owned.id).name)
            assertEquals(owner.id, responses.getValue(owned.id).owner?.id)
            assertEquals("Alex", responses.getValue(owned.id).owner?.name)
            assertEquals("Shared", responses.getValue(shared.id).name)
            assertNull(responses.getValue(shared.id).owner)
        } finally {
            accountRepository.deleteAllById(listOf(owned.id!!, shared.id!!))
            personRepository.deleteById(owner.id!!)
        }
    }
}
