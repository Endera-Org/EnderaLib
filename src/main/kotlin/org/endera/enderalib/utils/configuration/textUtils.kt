package org.endera.enderalib.utils.configuration

import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlNamingStrategy
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal fun String.stripUtf8Bom(): String =
    if (startsWith('﻿')) substring(1) else this

internal fun configYaml(strict: Boolean) = YamlConfiguration(
    strictMode = strict,
    breakScalarsAt = 400,
    yamlNamingStrategy = YamlNamingStrategy.KebabCase
)

private val timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")

/** `config.yml` with marker `-backup-` becomes `config-backup-2026-01-31_12-00-00.yml` in the same directory. */
internal fun File.timestampedSibling(marker: String): File {
    val suffix = if (extension.isNotBlank()) ".$extension" else ""
    return resolveSibling("$nameWithoutExtension$marker${LocalDateTime.now().format(timestampFormat)}$suffix")
}
