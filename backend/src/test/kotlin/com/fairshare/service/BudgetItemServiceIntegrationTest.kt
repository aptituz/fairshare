/*
 * Copyright (C) 2026 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.model.BudgetItem
import com.fairshare.model.BudgetItemType
import com.fairshare.model.Category
import com.fairshare.model.Person
import com.fairshare.repo.BudgetItemRepository
import com.fairshare.repo.CategoryRepository
import com.fairshare.repo.PersonRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:budget-item-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.liquibase.enabled=false",
    ],
)
class BudgetItemServiceIntegrationTest(
    @Autowired private val service: BudgetItemService,
    @Autowired private val budgetService: BudgetService,
    @Autowired private val itemRepository: BudgetItemRepository,
    @Autowired private val categoryRepository: CategoryRepository,
    @Autowired private val personRepository: PersonRepository,
) {
    @Test
    fun `all list filters map lazy associations without a caller transaction`() {
        val category = categoryRepository.save(Category(name = "Rent", type = BudgetItemType.EXPENSE, rank = 1))
        val person = personRepository.save(Person(name = "Alex", username = "alex"))
        val item =
            itemRepository.save(
                BudgetItem(
                    name = "Rent",
                    amount = BigDecimal("100"),
                    type = BudgetItemType.EXPENSE,
                    startDate = LocalDate.of(2026, 1, 1),
                    category = category,
                    person = person,
                ),
            )
        item.rootBudgetItem = item
        itemRepository.save(item)

        try {
            for (type in listOf(null, BudgetItemType.EXPENSE)) {
                for (month in listOf(null, "2026-10")) {
                    val response = service.list(type, month).single()
                    assertEquals(category.id, response.category?.id)
                    assertEquals("Rent", response.category?.name)
                    assertEquals(person.id, response.person?.id)
                    assertEquals("Alex", response.person?.name)
                }
            }
            assertEquals(0, service.list(BudgetItemType.INCOME, "2026-10").size)
            assertEquals(0, service.list(null, "2025-12").size)
            assertEquals("Rent", service.historyWithSuspensions(item.id!!).single().category?.name)
            val summary = budgetService.monthlySummary(YearMonth.of(2026, 10))
            assertEquals("Rent", summary.expensesByCategory.single().categoryName)
            assertEquals("Alex", summary.expensesByBudgetItem.single().personName)
        } finally {
            itemRepository.deleteById(item.id!!)
            categoryRepository.deleteById(category.id!!)
            personRepository.deleteById(person.id!!)
        }
    }
}
