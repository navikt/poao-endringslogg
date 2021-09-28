package no.bekk.env

val DB_USERNAME: String = System.getenv("DB_USERNAME")
val DB_PASSWORD: String = System.getenv("DB_PASSWORD")
val DB_HOST: String = System.getenv("DB_HOST")
val DB_DATABASE: String = System.getenv("DB_DATABASE")
val DB_PORT: Int = System.getenv("DB_PORT").toInt()
val SANITY_PROJECT_ID: String = System.getenv("SANITY_PROJECT_ID")
val SANITY_DATASET: String = System.getenv("SANITY_DATASET")
val CORS_ALLOWED_HOST: String = System.getenv("CORS_ALLOWED_HOST")
val BACKEND_PORT: Int = System.getenv("BACKEND_PORT").toInt()