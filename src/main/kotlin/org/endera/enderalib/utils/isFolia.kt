package org.endera.enderalib.utils

internal fun isFolia(): Boolean {
    try {
        Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
        return true
    } catch (e: ClassNotFoundException) {
        return false
    }
}