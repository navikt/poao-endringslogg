package no.pto.env

val DB_USERNAME: String = System.getenv("DB_USERNAME")
val DB_PASSWORD: String = System.getenv("DB_PASSWORD")
val DB_HOST: String = System.getenv("DB_HOST")
val DB_DATABASE: String = System.getenv("DB_DATABASE")
val DB_PORT: Int = System.getenv("DB_PORT").toInt()
val SANITY_PROJECT_ID: String = "li581mqu"

fun erIDev() = System.getenv("NAIS_CLUSTER_NAME") == "dev-gcp"
fun erIProd() = System.getenv("NAIS_CLUSTER_NAME") == "prod-gcp"
