package cz.jeme.mrthvolium.persistence

import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType
import java.nio.ByteBuffer
import java.util.*


object UUIDPersistentDataType : PersistentDataType<ByteArray, UUID> {
    override fun getPrimitiveType() = ByteArray::class.java

    override fun getComplexType() = UUID::class.java

    override fun toPrimitive(
        complex: UUID,
        context: PersistentDataAdapterContext
    ) = ByteBuffer.allocate(2 * Long.SIZE_BYTES)
        .putLong(complex.mostSignificantBits)
        .putLong(complex.leastSignificantBits)
        .array()

    override fun fromPrimitive(
        primitive: ByteArray,
        context: PersistentDataAdapterContext
    ): UUID {
        val buffer = ByteBuffer.wrap(primitive)
        return UUID(buffer.getLong(), buffer.getLong())
    }
}