/*
 * Copyright (C) 2025 Patrick Schoenfeld <patrick.schoenfeld@gmail.com>
 * SPDX-License-Identifier: GPL-3.0-only
 */

package com.fairshare.importer

import java.nio.file.Path

interface BankImporter {
    fun canHandle(filePath: Path): Boolean

    fun parse(filePath: Path): List<StandardTransaction>

    fun getBankName(): String
}
