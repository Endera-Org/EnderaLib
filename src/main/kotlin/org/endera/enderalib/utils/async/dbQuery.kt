package org.endera.enderalib.utils.async

import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

@Suppress("unused")
suspend fun <T> dbQuery(block: suspend () -> T): T =
    withIo {
        suspendTransaction { block() }
    }
