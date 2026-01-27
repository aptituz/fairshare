/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.controller

import com.fairshare.dto.ImportHistoryResponse
import com.fairshare.dto.ImportPreviewResponse
import com.fairshare.dto.ImportStatusResponse
import com.fairshare.dto.ImportTransactionsResponse
import com.fairshare.service.BankImportService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/import")
@Tag(name = "Import", description = "Import bank transactions.")
class ImportController(
    private val bankImportService: BankImportService,
) {
    @PostMapping(
        "/transactions",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
    )
    @Operation(summary = "Upload a CSV file and queue a transaction import")
    fun importTransactions(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("person_id", required = false) personId: Long?,
    ): ImportTransactionsResponse = bankImportService.queueImport(file, personId)

    @GetMapping("/status/{batchId}")
    @Operation(summary = "Check the status of an import batch")
    fun getStatus(
        @PathVariable batchId: Long,
    ): ImportStatusResponse = bankImportService.getStatus(batchId)

    @PostMapping(
        "/preview",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
    )
    @Operation(summary = "Preview the first 10 transactions in a CSV file")
    fun previewImport(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("person_id", required = false) personId: Long?,
    ): ImportPreviewResponse = bankImportService.preview(file, personId)

    @GetMapping("/history")
    @Operation(summary = "List previous import batches")
    fun history(
        @RequestParam("person_id", required = false) personId: Long?,
        @RequestParam("limit", defaultValue = "50") limit: Int,
    ): ImportHistoryResponse = bankImportService.history(personId, limit)

    @DeleteMapping("/{batchId}")
    @Operation(summary = "Rollback a completed import batch")
    fun rollback(
        @PathVariable batchId: Long,
    ): Map<String, Any> {
        val deleted = bankImportService.rollback(batchId)
        return mapOf(
            "success" to true,
            "deleted_transactions" to deleted,
        )
    }
}
