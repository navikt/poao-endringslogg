package no.pto.model

import kotlinx.serialization.Serializable
import no.pto.BlockContent

@Serializable
data class SystemmeldingSanityRespons(
    val result: List<SystemmeldingSanity>
)

@Serializable
data class SystemmeldingSanity(
    val tittel: String,
    val alert: String,
    val description: BlockContent,
    val tilDato: String?,
    val fraDato: String?,
)

@Serializable
data class Systemmelding(
    val tittel: String,
    val type: String,
    val beskrivelse: BlockContent
)