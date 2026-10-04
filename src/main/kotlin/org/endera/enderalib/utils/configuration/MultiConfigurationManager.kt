package org.endera.enderalib.utils.configuration

import kotlin.reflect.KClass

@Suppress("unused")
class MultiConfigurationManager(private val managers: List<ConfigurationManager<*>>) {
    fun loadAllConfigs(): Map<KClass<*>, Any> =
        managers.associate { it.type to it.loadOrCreateConfig() }
}
