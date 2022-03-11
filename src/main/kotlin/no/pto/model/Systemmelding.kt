package no.pto.model

import kotlinx.serialization.Serializable
import no.pto.BlockContent

@Serializable
data class SystemmeldingSanityRespons(
    val result: List<SystemmeldingSanity>
)

@Serializable
data class SystemmeldingSanity(
    val title: String,
    val alert: String,
    val description: BlockContent,
    val tilDato: String? = null,
    val fraDato: String? = null,
)

@Serializable
data class Systemmelding(
    val tittel: String,
    val type: String,
)