package org.endera.enderalib.utils.configuration

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Moves a configuration file that could not be loaded to `<name>_invalid_<timestamp>.<ext>`,
 * so a fresh one can be generated while the old one is kept for recovery.
 *
 * @throws java.io.IOException if the file could not be moved.
 */
fun renameInvalidConfig(file: File) {
    if (!file.exists()) return
    Files.move(file.toPath(), file.timestampedSibling("_invalid_").toPath(), StandardCopyOption.REPLACE_EXISTING)
}
