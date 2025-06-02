package cz.jeme.mrthvolium.persistence

import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import java.nio.ByteBuffer

object VectorPersistentDataType : PersistentDataType<ByteArray, Vector> {
    override fun getPrimitiveType() = ByteArray::class.java

    override fun getComplexType() = Vector::class.java

    override fun toPrimitive(
        complex: Vector,
        context: PersistentDataAdapterContext
    ) = ByteBuffer.allocate(3 * Double.SIZE_BYTES)
        .putDouble(complex.x)
        .putDouble(complex.y)
        .putDouble(complex.z)
        .array()

    override fun fromPrimitive(
        primitive: ByteArray,
        context: PersistentDataAdapterContext
    ): Vector {
        val buffer = ByteBuffer.wrap(primitive)
        return Vector(buffer.getDouble(), buffer.getDouble(), buffer.getDouble())
    }
}