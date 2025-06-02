package cz.jeme.mrthvolium.util

import cz.jeme.mrthvolium.persistence.PersistentData
import io.papermc.paper.persistence.PersistentDataViewHolder
import org.bukkit.inventory.ItemStack

inline val ItemStack?.isMenuItem: Boolean
    get() = !isNullOrEmpty && PersistentData.IS_MENU_ITEM.read(this as PersistentDataViewHolder) ?: false

inline val ItemStack?.isNullOrEmpty: Boolean
    get() = this == null || isEmpty