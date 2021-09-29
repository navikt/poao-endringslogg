package no.bekk.env

val DB_USERNAME: String = System.getenv("db_USERNAME")
val DB_PASSWORD: String = System.getenv("db_PASSWORD")
val DB_HOST: String = System.getenv("db_HOST")
val DB_DATABASE: String = System.getenv("db_DATABASE")
val DB_PORT: Int = System.getenv("db_PORT").toInt()
val SANITY_PROJECT_ID: String = "li581mqu"