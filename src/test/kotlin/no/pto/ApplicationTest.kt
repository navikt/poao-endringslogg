package no.pto

import org.junit.Test
import java.time.ZonedDateTime

class ApplicationTest {
    @Test
    fun filterOnUtcString() {
        val fraDatoFortid = "2020-03-03T13:26:08.422Z"
        val fraDatoFremtid = "3020-03-03T13:26:08.422Z"

        val datoListe = listOf(fraDatoFortid, fraDatoFremtid)

        val fortidsListe = datoListe
            .map(ZonedDateTime::parse)
            .filter { date -> date.isBefore(ZonedDateTime.now()) }
            .map(ZonedDateTime::toString)

        assert(fortidsListe.contains(fraDatoFortid))
    }
}