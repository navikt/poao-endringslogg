package no.pto

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import no.pto.model.SystemmeldingSanity
import no.pto.plugins.filtrerSystemmeldinger
import org.junit.Test

class ApplicationTest {
    @Test
    fun filterOnUtcString() {

        val fraDatoFortid = SystemmeldingSanity(
            title = "test1",
            alert = "",
            fraDato = "2020-03-03T13:26:08.422Z",
            description = Json.parseToJsonElement("{}").jsonObject
        )
        val fraDatoFremtid = SystemmeldingSanity(
            title = "test2",
            alert = "",
            fraDato = "3020-03-03T13:26:08.422Z",
            description = Json.parseToJsonElement("{}").jsonObject
        )

        val aktiveMeldinger = filtrerSystemmeldinger(listOf(fraDatoFortid, fraDatoFremtid))
        assert(aktiveMeldinger.find { it.tittel == fraDatoFortid.title } != null)
    }
}