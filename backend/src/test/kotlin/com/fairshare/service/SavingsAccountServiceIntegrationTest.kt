/*
 * Copyright (C) 2026 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.CreateSavingsAccountBalanceRequest
import com.fairshare.dto.UpdateSavingsAccountRequest
import com.fairshare.model.Person
import com.fairshare.model.SavingsAccount
import com.fairshare.repo.PersonRepository
import com.fairshare.repo.SavingsAccountRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.LocalDate

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
    @Autowired private val balanceService: SavingsAccountBalanceService,
    @Autowired private val dataTransferService: DataTransferService,
    @Autowired private val accountRepository: SavingsAccountRepository,
    @Autowired private val personRepository: PersonRepository,
) {
    @Test
    fun `account and balance operations map relations without a caller transaction`() {
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
            val updated =
                service.update(
                    owned.id!!,
                    UpdateSavingsAccountRequest("Renamed", owner.id, LocalDate.of(2026, 1, 1), null),
                )
            assertEquals("Renamed", updated.name)
            assertEquals("Alex", updated.owner?.name)
            val balance =
                balanceService.create(
                    owned.id!!,
                    CreateSavingsAccountBalanceRequest(LocalDate.of(2026, 10, 1), BigDecimal("100")),
                )
            try {
                assertEquals(owned.id, balanceService.listBalances().single().savingsAccountId)
                val monthlyBalances = balanceService.monthlyBalances(2026)
                assertEquals(
                    owned.id,
                    monthlyBalances.last { it.balances.isNotEmpty() }.balances.single().savingsAccountId,
                )
                val export = dataTransferService.exportData()
                assertEquals(owner.id, export.savingsAccounts.single { it.id == owned.id }.ownerPersonId)
                assertEquals(owned.id, export.savingsAccountBalances.single().savingsAccountId)
            } finally {
                balanceService.delete(balance.id!!)
            }
        } finally {
            accountRepository.deleteAllById(listOf(owned.id!!, shared.id!!))
            personRepository.deleteById(owner.id!!)
        }
    }
}
