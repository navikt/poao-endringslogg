val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val exposed_version: String by project
val DB_USERNAME: String = System.getenv("DB_USERNAME") ?: System.getenv("USER")
val DB_PASSWORD = System.getenv("DB_PASSWORD") ?: ""
val DB_HOST = System.getenv("DB_HOST") ?: "localhost"
val DB_DATABASE = System.getenv("DB_DATABASE") ?: "endringslogg"
val DB_PORT = System.getenv("DB_PORT") ?: "5432"

plugins {
    application
    kotlin("jvm") version "1.5.21"
    kotlin("plugin.serialization") version "1.5.20"
    id("org.flywaydb.flyway") version "8.0.0-beta2"
}

group = "no.bekk"
version = "0.0.1"
application {
    mainClass.set("no.bekk.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-client-core:1.6.2")
    implementation("io.ktor:ktor-client-cio:1.6.2")
    implementation("io.ktor:ktor-serialization:$ktor_version")
    implementation("io.ktor:ktor-client-serialization:$ktor_version")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed_version")
    implementation("org.postgresql:postgresql:42.2.23")
    implementation("com.google.cloud.sql:postgres-socket-factory:1.3.3")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")
    testImplementation("com.h2database:h2:1.3.148")
}

flyway {
    url = "jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_DATABASE?reWriteBatchedInserts=true?sslmode=require"
    driver = "org.postgresql.Driver"
    user = DB_USERNAME
    schemas = arrayOf("public")
    password = DB_PASSWORD
}
