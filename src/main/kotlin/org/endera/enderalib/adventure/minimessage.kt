package org.endera.enderalib.adventure

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage

val minimessage: MiniMessage = MiniMessage.miniMessage()

fun String.stringToComponent(): Component {
    return minimessage.deserialize(this)
}

@Suppress("unused")
fun Component.componentToString(): String {
    return minimessage.serialize(this)
}
