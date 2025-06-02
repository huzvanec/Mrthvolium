package cz.jeme.mrthvolium.persistence

import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType

object ItemStackPersistentDataType : PersistentDataType<ByteArray, ItemStack> {
    private val emptySerialized = byteArrayOf()

    override fun getPrimitiveType() = ByteArray::class.java

    override fun getComplexType() = ItemStack::class.java

    override fun toPrimitive(
        complex: ItemStack,
        context: PersistentDataAdapterContext
    ) = if (complex.isEmpty) emptySerialized else complex.serializeAsBytes()

    override fun fromPrimitive(
        primitive: ByteArray,
        context: PersistentDataAdapterContext
    ) = if (primitive.isEmpty()) ItemStack.empty()
    else ItemStack.deserializeBytes(primitive)
}