/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.repo

import com.fairshare.model.Person
import org.springframework.data.jpa.repository.JpaRepository

interface PersonRepository : JpaRepository<Person, Long> {
    fun existsByUsername(username: String): Boolean

    fun existsByUsernameAndIdNot(
        username: String,
        id: Long,
    ): Boolean

    fun findAllByOrderByIdAsc(): List<Person>

    fun findByUsername(username: String): Person?

    fun existsByPasswordHashIsNotNull(): Boolean
}
