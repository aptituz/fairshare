/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.service

import com.fairshare.dto.ImportHistoryResponse
import com.fairshare.dto.ImportPreviewResponse
import com.fairshare.dto.ImportStatusResponse
import com.fairshare.dto.ImportTransactionsResponse
import com.fairshare.dto.TransactionPreviewResponse
import com.fairshare.exception.BadRequestException
import com.fairshare.exception.ConflictException
import com.fairshare.exception.NotFoundException
import com.fairshare.mapper.toHistoryItem
import com.fairshare.model.ImportBatch
import com.fairshare.model.ImportBatchStatus
import com.fairshare.model.Person
import com.fairshare.repo.ImportBatchRepository
import com.fairshare.repo.PersonRepository
import com.fairshare.repo.TransactionRepository
import com.fairshare.util.HashingUtils
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

@Service
class BankImportService(
    private val importBatchRepository: ImportBatchRepository,
    private val personRepository: PersonRepository,
    private val transactionRepository: TransactionRepository,
    private val importProcessingService: ImportProcessingService,
    private val importHelperService: ImportHelperService,
) {
    fun queueImport(
        file: MultipartFile,
        personId: Long?,
    ): ImportTransactionsResponse {
        validateFile(file)
        val person = resolvePerson(personId)
        val fileHash = computeFileHash(file)
        if (isDuplicateFile(person, fileHash)) {
            throw ConflictException("This file has already been imported")
        }
        val tempFile = saveTempFile(file)
        val batch =
            importBatchRepository.save(
                ImportBatch(
                    person = person,
                    fileName = file.originalFilename ?: "transactions.csv",
                    fileHash = fileHash,
                    status = ImportBatchStatus.PENDING,
                ),
            )
        importProcessingService.processImportAsync(batch.id ?: error("Batch id missing"), tempFile)
        return ImportTransactionsResponse(
            success = true,
            batchId = batch.id ?: 0,
            status = batch.status.name.lowercase(),
            message = "Import queued for processing",
        )
    }

    fun getStatus(batchId: Long): ImportStatusResponse {
        val batch =
            importBatchRepository.findById(batchId).orElseThrow {
                NotFoundException("Import batch $batchId not found")
            }
        return ImportStatusResponse(
            batchId = batch.id ?: 0,
            status = batch.status.name.lowercase(),
            recordCount = batch.recordCount,
            errorMessage = batch.errorMessage,
        )
    }

    fun preview(
        file: MultipartFile,
        personId: Long?,
    ): ImportPreviewResponse {
        validateFile(file)
        resolvePerson(personId)
        val tempFile = saveTempFile(file)
        return try {
            val importer = importHelperService.detectImporter(tempFile)
            val transactions = importer.parse(tempFile)
            ImportPreviewResponse(
                bank = importer.getBankName(),
                totalCount = transactions.size,
                preview =
                    transactions
                        .take(10)
                        .map {
                            TransactionPreviewResponse(
                                date = it.bookingDate,
                                amount = it.amount,
                                description = importHelperService.buildDescription(it),
                            )
                        },
            )
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    fun history(
        personId: Long?,
        limit: Int,
    ): ImportHistoryResponse {
        val pageSize = limit.coerceIn(1, 200)
        val pageable = PageRequest.of(0, pageSize)
        val batches =
            if (personId != null) {
                resolvePerson(personId)
                importBatchRepository.findAllByPersonIdOrderByImportDateDesc(personId, pageable)
            } else {
                importBatchRepository.findAllByOrderByImportDateDesc(pageable)
            }
        return ImportHistoryResponse(batches.map { it.toHistoryItem() })
    }

    @Transactional
    fun rollback(batchId: Long): Long {
        val batch =
            importBatchRepository.findById(batchId).orElseThrow {
                NotFoundException("Import batch $batchId not found")
            }
        if (batch.status == ImportBatchStatus.PROCESSING) {
            throw ConflictException("Cannot rollback a processing import batch")
        }
        val count = transactionRepository.countByImportBatchId(batchId)
        transactionRepository.deleteByImportBatchId(batchId)
        importBatchRepository.delete(batch)
        return count
    }

    private fun validateFile(file: MultipartFile) {
        if (file.isEmpty) {
            throw BadRequestException("File is empty")
        }
        if (file.size > MAX_FILE_SIZE_BYTES) {
            throw BadRequestException("File exceeds 10MB")
        }
        val fileName = file.originalFilename?.lowercase().orEmpty()
        val contentType = file.contentType?.lowercase().orEmpty()
        if (!fileName.endsWith(".csv") && !contentType.contains("csv")) {
            throw BadRequestException("File must be a CSV")
        }
    }

    private fun resolvePerson(personId: Long?): Person? =
        if (personId == null) {
            null
        } else {
            personRepository.findById(personId).orElseThrow {
                NotFoundException("Person $personId not found")
            }
        }

    private fun isDuplicateFile(
        person: Person?,
        fileHash: String,
    ): Boolean =
        if (person == null) {
            importBatchRepository.existsByPersonIdIsNullAndFileHash(fileHash)
        } else {
            importBatchRepository.existsByPersonIdAndFileHash(person.id ?: 0, fileHash)
        }

    private fun saveTempFile(file: MultipartFile): Path {
        val suffix = file.originalFilename?.let { "-$it" } ?: "-transactions.csv"
        val tempFile = Files.createTempFile("import", suffix)
        file.inputStream.use { input ->
            Files.copy(input, tempFile, StandardCopyOption.REPLACE_EXISTING)
        }
        return tempFile
    }

    private fun computeFileHash(file: MultipartFile): String = HashingUtils.sha256Hex(file.bytes)

    companion object {
        private const val MAX_FILE_SIZE_BYTES: Long = 10 * 1024 * 1024
    }
}
