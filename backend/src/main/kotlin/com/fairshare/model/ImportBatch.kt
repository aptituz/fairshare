/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "import_batches")
class ImportBatch(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    var person: Person? = null,
    @Column(name = "import_date", nullable = false)
    var importDate: LocalDateTime = LocalDateTime.now(),
    @Column(name = "file_name", nullable = false)
    var fileName: String,
    @Column(name = "file_hash", nullable = false, length = 64)
    var fileHash: String,
    @Column(name = "record_count")
    var recordCount: Int? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: ImportBatchStatus = ImportBatchStatus.PENDING,
    @Column(name = "error_message")
    var errorMessage: String? = null,
)
