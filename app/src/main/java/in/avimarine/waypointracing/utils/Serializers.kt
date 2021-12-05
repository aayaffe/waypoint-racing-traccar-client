package `in`.avimarine.waypointracing.utils

import `in`.avimarine.waypointracing.route.Finish
import `in`.avimarine.waypointracing.route.Gate
import `in`.avimarine.waypointracing.route.Waypoint
import android.location.Location
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import java.util.*

class Serializers {
    companion object{
        object UUIDSerializer : KSerializer<UUID> {
            override val descriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

            override fun deserialize(decoder: Decoder): UUID {
                return UUID.fromString(decoder.decodeString())
            }

            override fun serialize(encoder: Encoder, value: UUID) {
                encoder.encodeString(value.toString())
            }
        }
        object DateSerializer: KSerializer<Date>{
            override val descriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)

            override fun deserialize(decoder: Decoder): Date {
                return Date(decoder.decodeLong())
            }

            override fun serialize(encoder: Encoder, value: Date) {
                encoder.encodeLong(value.time)
            }

        }
        object LocationSerializer: KSerializer<Location>{
            override val descriptor = buildClassSerialDescriptor("Location") {
                element<Double>("lat")
                element<Double>("lon")
            }

            override fun deserialize(decoder: Decoder): Location = decoder.decodeStructure(descriptor) {
                var lat = -1.0
                var lon = -1.0
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> lat = decodeDoubleElement(descriptor, 0)
                        1 -> lon = decodeDoubleElement(descriptor, 1)
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
                require(lat in -90.0..90.0 && lon in -180.0..180.0)
                val l = Location("")
                l.latitude = lat
                l.longitude = lon
                return l
            }

            override fun serialize(encoder: Encoder, value: Location) = encoder.encodeStructure(descriptor) {
                encodeDoubleElement(descriptor, 0, (value.latitude))
                encodeDoubleElement(descriptor, 1, (value.longitude))
            }

        }

        @Serializer(forClass = Gate::class)
        object GateSerializer {}

        @Serializer(forClass = Waypoint::class)
        object WaypointSerializer {}

        @Serializer(forClass = Finish::class)
        object FinishSerializer {}

//        private val serialModule = SerializersModule {
//            polymorphic(RouteElement::class) {
//                Waypoint::class with WaypointSerializer
//                Gate::class with GateSerializer
//                Finish::class with FinishSerializer
//            }
//        }
//        val format = Json { serializersModule = serialModule }
    }
}