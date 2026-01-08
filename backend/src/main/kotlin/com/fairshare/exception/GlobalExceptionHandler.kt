/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(ex: NotFoundException, request: WebRequest): ResponseEntity<Any> {
        val body = mapOf("message" to ex.message)
        return ResponseEntity(body, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(ConflictException::class)
    fun handleConflictException(ex: ConflictException, request: WebRequest): ResponseEntity<Any> {
        val body = mapOf("message" to ex.message)
        return ResponseEntity(body, HttpStatus.CONFLICT)
    }

    @ExceptionHandler(BadRequestException::class)
    fun handleBadRequestException(ex: BadRequestException, request: WebRequest): ResponseEntity<Any> {
        val body = mapOf("message" to ex.message)
        return ResponseEntity(body, HttpStatus.BAD_REQUEST)
    }
}
