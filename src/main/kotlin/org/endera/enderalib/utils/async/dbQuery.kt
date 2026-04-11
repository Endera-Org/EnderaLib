package org.endera.enderalib.utils.async

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

@Suppress("unused")
suspend fun <T> dbQuery(block: suspend () -> T): T {
    return withContext(ioDispatcher) {
        suspendTransaction { block() }
    }
}
