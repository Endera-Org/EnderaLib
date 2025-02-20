package org.endera.enderalib.utils

import org.bukkit.NamespacedKey
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

@Suppress("unused")
fun ItemMeta.getByKey(key: NamespacedKey): String? {
    return this.persistentDataContainer.get(key, PersistentDataType.STRING)
}