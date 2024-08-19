val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val exposed_version: String by project
val logstash_encoder_version: String by project

plugins {
    application
    kotlin("plugin.serialization") version "2.0.0"
    kotlin("jvm") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "no.nav.pto"
version = "0.0.1"
application {
    mainClass.set("no.pto.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-cors:${ktor_version}")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-serialization:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstash_encoder_version")
    implementation("org.jetbrains.exposed:exposed-core:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-dao:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposed_version")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposed_version")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.google.cloud.sql:postgres-socket-factory:1.19.0")
    implementation("org.flywaydb:flyway-core:10.15.2")
    implementation("org.flywaydb:flyway-database-postgresql:10.17.1")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("com.launchdarkly:okhttp-eventsource:4.1.1")
    implementation("com.zaxxer:HikariCP:5.1.0")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")
    testImplementation("com.h2database:h2:2.2.224")
}

tasks{
    shadowJar {
        manifest {
            attributes(Pair("Main-Class", "no.pto.ApplicationKt"))
        }
    }
}
