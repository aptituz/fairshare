/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.repo

import com.fairshare.model.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByTokenHash(tokenHash: String): RefreshToken?

    @Modifying
    @Query("update RefreshToken token set token.revokedAt = :revokedAt where token.personId = :personId and token.revokedAt is null")
    fun revokeAllForPerson(
        @Param("personId") personId: Long,
        @Param("revokedAt") revokedAt: Instant,
    ): Int
}
