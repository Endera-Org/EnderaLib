package org.endera.enderalib.utils.configuration

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
private data class Limits(val maxHomes: Int, val cooldown: Int)

@Serializable
private data class TestConfig(
    val serverName: String,
    val limits: Limits,
    val worlds: Map<String, Limits>,
    val motd: String?,
)

private val default = TestConfig(
    serverName = "default",
    limits = Limits(maxHomes = 3, cooldown = 10),
    worlds = mapOf("world" to Limits(maxHomes = 1, cooldown = 1)),
    motd = "hello",
)

private fun merge(yaml: String): TestConfig =
    mergeYamlConfigs(yaml.trimIndent(), default, TestConfig.serializer())

class MergerTest {

    @Test
    fun `missing keys are filled from the default and unknown keys are dropped`() {
        val merged = merge(
            """
            server-name: custom
            removed-option: true
            limits:
              max-homes: 5
            """
        )

        assertEquals(default.copy(serverName = "custom", limits = Limits(maxHomes = 5, cooldown = 10)), merged)
    }

    @Test
    fun `keys written in another case are migrated`() {
        val merged = merge(
            """
            serverName: custom
            limits:
              max_homes: 5
            """
        )

        assertEquals("custom", merged.serverName)
        assertEquals(5, merged.limits.maxHomes)
    }

    @Test
    fun `map entries removed by the owner are not restored`() {
        val merged = merge(
            """
            worlds:
              mining:
                max-homes: 2
                cooldown: 2
            """
        )

        assertEquals(mapOf("mining" to Limits(maxHomes = 2, cooldown = 2)), merged.worlds)
    }

    @Test
    fun `map entries shared with the default are patched`() {
        val merged = merge(
            """
            worlds:
              world:
                max-homes: 9
            """
        )

        assertEquals(mapOf("world" to Limits(maxHomes = 9, cooldown = 1)), merged.worlds)
    }

    @Test
    fun `null is kept only where the schema allows it`() {
        val merged = merge(
            """
            server-name: null
            motd: null
            """
        )

        assertEquals("default", merged.serverName)
        assertEquals(null, merged.motd)
    }

    @Test
    fun `values of the wrong shape fall back to the default`() {
        val merged = merge(
            """
            server-name: custom
            limits: oops
            worlds: [a, b]
            """
        )

        assertEquals(default.copy(serverName = "custom"), merged)
    }
}
