import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import no.bekk.Modal
import no.bekk.Slide
import java.util.*


@ExperimentalSerializationApi
@Serializer(forClass = UUID::class)
object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UUID) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): UUID {
        return UUID.fromString(decoder.decodeString())
    }
}

@ExperimentalSerializationApi
@Serializer(forClass = Modal::class)
object ModalSerializer : KSerializer<Modal?> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("Modal") {
            element<String>("slideHeader")
            element<List<Slide>>("slides")
        }

    override fun serialize(encoder: Encoder, value: Modal?) {
        if (value == null) {
            encoder.encodeNull()
            return
        }
        encoder.encodeSerializableValue(
            JsonObject.serializer(), JsonObject(
                mapOf(
                    "header" to JsonPrimitive(value.title),
                    "slides" to JsonArray(value.slides.map(Json::encodeToJsonElement))
                )
            )
        )
    }

    override fun deserialize(decoder: Decoder): Modal? {
        val modal = decoder.decodeSerializableValue(JsonObject.serializer())
        val numSlides = modal["numSlides"]?.jsonPrimitive?.int ?: return null
        if (numSlides < 1) return null
        val header = modal["modalHeader"]?.jsonPrimitive?.content ?: "Ny oppdatering"
        val slides = (1..numSlides).map { "modalSlide$it" }.mapNotNull { modal[it]?.jsonObject }.map {
            Json.decodeFromJsonElement(Slide.serializer(), it)
        }
        return Modal(header, slides)
    }
}
