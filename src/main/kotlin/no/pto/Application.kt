package no.pto

import SanityClient
import io.ktor.application.*
import io.ktor.serialization.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json
import no.pto.config.Every
import no.pto.config.Scheduler
import no.pto.database.connectToDatabase
import no.pto.env.*
import no.pto.plugins.*
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger("no.nav.pto.endringslogg.Application")
private val client = SanityClient(SANITY_PROJECT_ID, API_VERSION_ENDRINGSLOGG)
private val scheduler = Scheduler { client.reconnectListening() }

fun Application.main() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            encodeDefaults = true
        })
    }
    install(CORS) {
        host("app.adeo.no", listOf("https"))
        host("veilarbportefoljeflatefs.nais.adeo.no", listOf("https"))
        host("veilarbportefoljeflatefs-q1.nais.preprod.local", listOf("https"))
        host("app-q1.adeo.no", listOf("https"))
        host("app-q1.dev.adeo.no", listOf("https"))
        host("veilarbportefoljeflate.intern.dev.nav.no", listOf("https"))
        host("veilarbportefoljeflate.intern.nav.no", listOf("https"))

        header("Nav-Consumer-Id")
        header("Content-Type")

        method(HttpMethod.Options)
        method(HttpMethod.Get)
        method(HttpMethod.Post)
        method(HttpMethod.Patch)
        header(HttpHeaders.ContentType)
    }
}

fun main() {
    logger.info("Kjører flyway")
    val flyway: Flyway = Flyway.configure().dataSource(
        "jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_DATABASE?reWriteBatchedInserts=true?sslmode=require",
        DB_USERNAME,
        DB_PASSWORD
    ).load()
    flyway.migrate()
    connectToDatabase()

    // NOTE: Legg til evt. nye queries her ->
    client.initSanitySystemMeldingListener()
    client.initSanityEndringloggListener()

    // Hvis sanity er nede vil vi prøve å starte lytting vert 10ene min
    // Dette kan også Skje ved oppstart av flere enn 1 pod
    scheduler.scheduleExecution(Every(10, TimeUnit.MINUTES))

    embeddedServer(Netty, environment = applicationEngineEnvironment {
        module {
            main()
            configureEndringsloggRouting(client)
            configureSystemmeldingRouting(client)
        }
        connector {
            port = 8080
            host = "0.0.0.0"
        }
    }) {
    }.start(wait = true)
}
