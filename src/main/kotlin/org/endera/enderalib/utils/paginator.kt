package org.endera.enderalib.utils

@Deprecated("Use chunked directly", ReplaceWith("chunked(pageSize)"))
@Suppress("unused")
fun <T> List<T>.paginate(pageSize: Int): List<List<T>> = this.chunked(pageSize)
