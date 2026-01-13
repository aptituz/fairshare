/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.controller

import com.fairshare.dto.DataExportPayload
import com.fairshare.service.DataTransferService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/data")
@Tag(name = "Data", description = "Export and import all data.")
class DataTransferController(
    private val dataTransferService: DataTransferService,
) {
    @GetMapping("/export")
    @Operation(summary = "Export all data")
    fun exportData(): DataExportPayload = dataTransferService.exportData()

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Import all data (overwrites existing records)")
    fun importData(
        @RequestBody payload: DataExportPayload,
    ) {
        dataTransferService.importData(payload)
    }
}
