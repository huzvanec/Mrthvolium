package cz.jeme.mrthvolium.persistence

import com.mojang.authlib.properties.Property
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType
import java.nio.ByteBuffer
import java.util.*

object CorpseSkinPersistentDataType : PersistentDataType<ByteArray, Property> {
    private val decoder = Base64.getDecoder()
    private val encoder = Base64.getEncoder()

    override fun getPrimitiveType() = ByteArray::class.java

    override fun getComplexType() = Property::class.java

    override fun toPrimitive(
        complex: Property,
        context: PersistentDataAdapterContext
    ): ByteArray {
        val value = decoder.decode(complex.value)
        val signature = decoder.decode(complex.signature)

        return ByteBuffer.allocate(Int.SIZE_BYTES + value.size + signature.size)
            .putInt(value.size)
            .put(value)
            .put(signature)
            .array()
    }

    override fun fromPrimitive(
        primitive: ByteArray,
        context: PersistentDataAdapterContext
    ): Property {
        val buffer = ByteBuffer.wrap(primitive)
        val valueSize = buffer.int
        val value = ByteArray(valueSize)
        buffer.get(value)
        val signature = ByteArray(primitive.size - valueSize - Int.SIZE_BYTES)
        buffer.get(signature)
        return Property(
            "textures",
            encoder.encodeToString(value),
            encoder.encodeToString(signature)
        )
    }
}