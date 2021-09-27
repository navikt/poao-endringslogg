package no.bekk

import SanityClient
import io.ktor.application.*
import io.ktor.serialization.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json
import no.bekk.env.*
import no.bekk.plugins.*

fun Application.main() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            encodeDefaults = true
        })
    }
    install(CORS) {
        // TODO: correctly define allowed hosts. Env. vars or secrets?
        host("localhost:3000")
        host("localhost:3333")
        host("localhost:6006")
        host(CORS_ALLOWED_HOST, schemes = listOf("http", "https"))
        method(HttpMethod.Options)
        method(HttpMethod.Get)
        method(HttpMethod.Post)
        method(HttpMethod.Put)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
        header(HttpHeaders.ContentType)
    }
}

fun main() {
    val client = SanityClient(SANITY_PROJECT_ID, SANITY_DATASET)

    connectToDatabase()

    embeddedServer(Netty, environment = applicationEngineEnvironment {
        module {
            main()
            configureRouting(client)
        }
        connector {
            port = BACKEND_PORT
            host = "0.0.0.0"
        }
    }) {
    }.start(wait = true)
}
