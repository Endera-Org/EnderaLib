import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm") version "2.4.20"
    kotlin("plugin.serialization") version "2.4.20"
    `maven-publish`
}

group = "org.endera"
version = "1.6.0"

val kotlinVersion = "2.4.20"
val exposedVersion = "1.5.0"
val ktorVersion = "3.6.0"

val apiLibraries = listOf(
    "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion",
    "io.ktor:ktor-client-core-jvm:$ktorVersion",
    "io.ktor:ktor-client-okhttp-jvm:$ktorVersion",
    "io.ktor:ktor-client-content-negotiation-jvm:$ktorVersion",
    "io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion",
    "org.jetbrains.exposed:exposed-core:$exposedVersion",
    "org.jetbrains.exposed:exposed-dao:$exposedVersion",
    "org.jetbrains.exposed:exposed-jdbc:$exposedVersion",
    "com.zaxxer:HikariCP:7.1.0",
    "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.11.0",
    "org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.11.0",
    "org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.11.0",
    "com.charleskorn.kaml:kaml-jvm:0.104.0",
)

val runtimeLibraries = listOf(
    "com.mysql:mysql-connector-j:26.7.0",
    "org.postgresql:postgresql:42.7.13",
    "org.mariadb.jdbc:mariadb-java-client:3.5.10",
    "com.h2database:h2:2.5.252",
)

val minecraftLibraries = apiLibraries + runtimeLibraries

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("net.kyori:adventure-text-minimessage:4.16.0")
    compileOnly("dev.folia:folia-api:1.20.4-R0.1-SNAPSHOT")

    apiLibraries.forEach { api(it) }
    runtimeLibraries.forEach { runtimeOnly(it) }

    testImplementation(kotlin("test"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "org.endera.enderalib"
            artifactId = "enderalib"
            version = version

            from(components["java"])
        }
    }
}


tasks.processResources {
    inputs.property("version", rootProject.version)
    inputs.property("minecraftLibraries", minecraftLibraries)

    filesMatching("**/plugin.yml") {
        expand(
            "version" to rootProject.version,
            "libraries" to minecraftLibraries.joinToString("\n") { "  - $it" },
        )
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_1)
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

tasks.withType<JavaCompile> {
    targetCompatibility = "17"
}
