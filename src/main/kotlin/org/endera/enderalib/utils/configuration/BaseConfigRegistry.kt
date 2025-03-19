package org.endera.enderalib.utils.configuration

import kotlin.reflect.KClass

@Suppress("unused")
abstract class BaseConfigRegistry {
    private val configs = mutableMapOf<KClass<*>, Any>()

    fun <T : Any> register(configClass: KClass<T>, config: T) {
        configs[configClass] = config
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(configClass: KClass<T>): T? {
        return configs[configClass] as? T
    }
}
