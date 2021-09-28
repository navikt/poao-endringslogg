package no.bekk.env

val DB_USERNAME: String = System.getenv("DB_USERNAME")
val DB_PASSWORD: String = System.getenv("DB_PASSWORD")
val DB_HOST: String = System.getenv("DB_HOST")
val DB_DATABASE: String = System.getenv("DB_DATABASE")
val DB_PORT: Int = System.getenv("DB_PORT").toInt()
val SANITY_PROJECT_ID: String = "li581mqu"