val exposedVersion = "1.5.0"
val ktorVersion = "3.6.0"
val logbackVersion = "1.6.5"
val logstashEncoderVersion = "9.0"

plugins {
    val kotlinVersion = "2.4.20"
    application
    kotlin("plugin.serialization") version kotlinVersion
    kotlin("jvm") version kotlinVersion
    id("com.gradleup.shadow") version "9.6.1"
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
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.postgresql:postgresql:42.7.13")
    implementation("org.flywaydb:flyway-core:13.8.1")
    implementation("org.flywaydb:flyway-database-postgresql:13.8.1")
    implementation("com.github.ben-manes.caffeine:caffeine:3.3.0")
    implementation("com.launchdarkly:okhttp-eventsource:4.3.0")
    implementation("com.zaxxer:HikariCP:7.1.0")
    implementation(platform("tools.jackson:jackson-bom:3.2.3")) // brukes av logstash, men setter versjon her for å unngå høy sårbarhet
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation(kotlin("test"))
}

tasks{
    shadowJar {
        mergeServiceFiles()
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        manifest {
            attributes(Pair("Main-Class", "no.pto.ApplicationKt"))
        }
    }
}
