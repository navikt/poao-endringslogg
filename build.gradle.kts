val exposedVersion = "1.0.0"
val ktorVersion = "3.4.0"
val logbackVersion = "1.5.3"
val logstashEncoderVersion = "7.4"

plugins {
    val kotlinVersion = "2.3.10"
    application
    kotlin("plugin.serialization") version kotlinVersion
    kotlin("jvm") version kotlinVersion
    id("com.gradleup.shadow") version "9.3.1"
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
    implementation("org.postgresql:postgresql:42.7.10")
    implementation("org.flywaydb:flyway-core:12.0.1")
    implementation("org.flywaydb:flyway-database-postgresql:12.0.1")
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.3")
    implementation("com.launchdarkly:okhttp-eventsource:4.2.0")
    implementation("com.zaxxer:HikariCP:7.0.2")
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
