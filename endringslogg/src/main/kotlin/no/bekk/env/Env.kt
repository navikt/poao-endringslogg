package no.bekk.env

val DB_USERNAME: String = System.getenv("DB_USERNAME") ?: System.getenv("USER")
val DB_PASSWORD = System.getenv("DB_PASSWORD") ?: ""
val DB_HOST = System.getenv("DB_HOST") ?: "localhost"
val DB_DATABASE = System.getenv("DB_DATABASE") ?: "endringslogg"
val DB_PORT = System.getenv("DB_PORT") ?: "5432"
val SANITY_PROJECT_ID: String = System.getenv("SANITY_PROJECT_ID") ?: "li581mqu"
val SANITY_DATASET: String = System.getenv("SANITY_DATASET") ?: "production"
val CORS_ALLOWED_HOST = System.getenv("CORS_ALLOWED_HOST") ?:"localhost:3000"
val BACKEND_PORT = System.getenv("BACKEND_PORT")?.toIntOrNull() ?: 8080