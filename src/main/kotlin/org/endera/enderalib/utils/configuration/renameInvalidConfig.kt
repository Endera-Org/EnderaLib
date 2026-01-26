package org.endera.enderalib.utils.configuration

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Renames the provided configuration file by appending a timestamp and marking it as invalid.
 * This is typically used when an existing configuration file is found to be corrupt or cannot be loaded,
 * allowing the system to generate a new configuration file while preserving the old one for debugging or recovery.
 *
 * @param file The file object representing the existing configuration file to be renamed.
 */
fun renameInvalidConfig(file: File) {
    if (!file.exists()) return

    val timestamp = DateTimeFormatter
        .ofPattern("yyyy-MM-dd_HH-mm-ss")
        .withZone(ZoneId.systemDefault())
        .format(Instant.now())

    val newNameBase = buildString {
        append(file.nameWithoutExtension)
        append("_invalid_")
        append(timestamp)
    }

    val target = generateNonExistingSibling(
        file = file,
        nameWithoutExtension = newNameBase,
        extension = file.extension
    )

    try {
        Files.move(
            file.toPath(),
            target.toPath(),
            StandardCopyOption.ATOMIC_MOVE
        )
    } catch (_: Exception) {
        try {
            Files.move(
                file.toPath(),
                target.toPath()
            )
        } catch (_: Exception) {
            runCatching { file.copyTo(target, overwrite = false) }
            runCatching { file.delete() }
        }
    }
}

private fun generateNonExistingSibling(
    file: File,
    nameWithoutExtension: String,
    extension: String,
): File {
    val parent = file.parentFile ?: File(".")
    val ext = extension.takeIf { it.isNotBlank() }
    fun buildName(index: Int?): String = buildString {
        append(nameWithoutExtension)
        if (index != null) {
            append("_")
            append(index)
        }
        if (ext != null) {
            append(".")
            append(ext)
        }
    }

    var index: Int? = null
    while (true) {
        val candidate = File(parent, buildName(index))
        if (!candidate.exists()) return candidate
        index = (index ?: 0) + 1
    }
}
