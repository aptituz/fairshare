/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.exception

open class ApiException(message: String) : RuntimeException(message)

class NotFoundException(message: String) : ApiException(message)

class ConflictException(message: String) : ApiException(message)

class BadRequestException(message: String) : ApiException(message)
