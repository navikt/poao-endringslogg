package no.bekk

import ModalSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import no.bekk.env.*

@Serializable
data class EndringJson(val result: List<Endring>)

@Serializable
data class Endring(
    val title: String,
    val description: BlockContent,
    val date: String? = null,
    val linkAttributes: LinkAttributes? = null,
    @SerialName("_id") val id: String,
    val seen: Boolean = false,
    val modal: Modal? = null,
    val projectId: String = SANITY_PROJECT_ID,
    val dataset: String = SANITY_DATASET,
    val apiHost: String = "https://cdn.sanity.io"
)

@Serializable
data class LinkAttributes(val link: String? = null, val linkText: String? = null)

typealias BlockContent = JsonElement

@Serializable(with = ModalSerializer::class)
data class Modal(val title: String, val slides: List<Slide>)

@Serializable
data class Slide(
    @SerialName("slideHeader") val header: String,
    @SerialName("slideDescription") val description: BlockContent? = null,
    @SerialName("slideImage") val image: JsonElement? = null,
    @SerialName("altText") val altText: String? = null,
)

@Serializable
data class BrukerData(val userId: String, val appId: String, val maxEntries: Int)

@Serializable
data class SeenStatus(
    val userId: String,
    val appId: String,
    val documentIds: List<String>
)

@Serializable
data class SessionDuration(
    val userId: String,
    val appId: String,
    val duration: Int,
    val unseenFields: Int
)

@Serializable
data class SeenWithTime(
    val userId: String,
    val documentId: String,
    val timeStamp: String
)

@Serializable
data class SeenDataClass(
    val userId: String,
    val documentId: String,
    val openedLink: Boolean,
    val openedModal: Boolean,
    val timeStamp: String
)

@Serializable
data class UserSessionClass(
    val userId: String,
    val appId: String,
    val duration: Int,
    val unseenFields: Int,
    val timeStamp: String,
)

@Serializable
data class UniqueSessionsPerDay(
    val date: String,
    val users: Long
)

@Serializable
data class DocumentId(
    val documentId: String,
)
