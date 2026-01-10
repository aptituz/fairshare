/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.mapper

import com.fairshare.dto.PersonResponse
import com.fairshare.model.Person

fun Person.toResponse(): PersonResponse =
    PersonResponse(
        id = id,
        name = name,
        username = username,
    )
