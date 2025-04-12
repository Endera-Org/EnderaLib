import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    id("com.gradleup.shadow") version "8.3.6"
    `maven-publish`
}

group = "org.endera"
version = "1.4.5"

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
    val exposedVersion = "0.61.0"
    val ktorVersion = "3.1.2"

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

    api("com.zaxxer:HikariCP:6.3.0")

    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
    api("com.charleskorn.kaml:kaml:0.76.0")


    // Database drivers
    runtimeOnly("com.mysql:mysql-connector-j:9.2.0")
    runtimeOnly("org.postgresql:postgresql:42.7.5")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client:3.5.3")
    runtimeOnly("com.h2database:h2:2.3.232")
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