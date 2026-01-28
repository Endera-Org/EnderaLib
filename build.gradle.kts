import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.serialization") version "2.3.0"
    id("com.gradleup.shadow") version "9.2.2"
    `maven-publish`
}

group = "org.endera"
version = "1.4.8"

repositories {
    mavenCentral()
//    maven("https://repo.papermc.io/repository/maven-public/")
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    // Minecraft APIs
    val exposedVersion = "1.0.0-rc-4"
    val ktorVersion = "3.3.3"

    compileOnly("net.kyori:adventure-text-minimessage:4.16.0")

    compileOnly("dev.folia:folia-api:1.20.4-R0.1-SNAPSHOT")
//    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")

    // Exposed
    api("io.ktor:ktor-client-core:$ktorVersion")
    api("io.ktor:ktor-client-okhttp:$ktorVersion")
    api("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    api("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    api("org.jetbrains.exposed:exposed-core:$exposedVersion")
    api("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    api("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    api("com.zaxxer:HikariCP:7.0.2")

    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0")
    api("com.charleskorn.kaml:kaml:0.104.0")


    // Database drivers
    runtimeOnly("com.mysql:mysql-connector-j:9.5.0")
    runtimeOnly("org.postgresql:postgresql:42.7.8")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:3.5.7")
    runtimeOnly("com.h2database:h2:2.4.240")
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
        filesMatching("**plugin.yml") {
            expand("version" to rootProject.version)
    }
}

tasks.shadowJar {
    archiveClassifier.set("shaded")
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