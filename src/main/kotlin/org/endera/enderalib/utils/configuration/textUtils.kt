package org.endera.enderalib.utils.configuration

internal fun String.stripUtf8Bom(): String =
    if (startsWith('\uFEFF')) substring(1) else this

