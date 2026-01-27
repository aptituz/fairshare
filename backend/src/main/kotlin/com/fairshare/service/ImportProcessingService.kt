/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.exception.NotFoundException
import com.fairshare.model.ImportBatchStatus
import com.fairshare.model.Transaction
import com.fairshare.repo.ImportBatchRepository
import com.fairshare.repo.TransactionRepository
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path

@Service
class ImportProcessingService(
    private val importBatchRepository: ImportBatchRepository,
    private val transactionRepository: TransactionRepository,
    private val importHelperService: ImportHelperService,
) {
    @Async
    fun processImportAsync(
        batchId: Long,
        tempFile: Path,
    ) {
        try {
            val batch =
                importBatchRepository.findById(batchId).orElseThrow {
                    NotFoundException("Import batch $batchId not found")
                }
            batch.status = ImportBatchStatus.PROCESSING
            importBatchRepository.save(batch)
            val importer = importHelperService.detectImporter(tempFile)
            val transactions = importer.parse(tempFile)
            var inserted = 0
            transactions.forEach { standardTransaction ->
                val description = importHelperService.buildDescription(standardTransaction)
                val dedupKey =
                    importHelperService.deduplicationKey(
                        standardTransaction.bookingDate,
                        standardTransaction.amount,
                        description,
                    )
                if (!transactionRepository.existsByDeduplicationKey(dedupKey)) {
                    transactionRepository.save(
                        Transaction(
                            person = batch.person,
                            importBatch = batch,
                            date = standardTransaction.bookingDate,
                            amount = standardTransaction.amount,
                            description = description,
                            rawLine = standardTransaction.rawLine,
                            deduplicationKey = dedupKey,
                        ),
                    )
                    inserted++
                }
            }
            batch.status = ImportBatchStatus.COMPLETED
            batch.recordCount = inserted
            batch.errorMessage = null
            importBatchRepository.save(batch)
        } catch (ex: Exception) {
            val batch = importBatchRepository.findById(batchId).orElse(null)
            if (batch != null) {
                batch.status = ImportBatchStatus.FAILED
                batch.errorMessage = ex.message ?: "Import failed"
                importBatchRepository.save(batch)
            }
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }
}
