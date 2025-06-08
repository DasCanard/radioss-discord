plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    id("com.gradleup.shadow") version "8.3.0"
}

group = "me.richy.radioss"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://m2.dv8tion.net/releases")
}

dependencies {
    // Discord JDA für Discord Bot
    implementation("net.dv8tion:JDA:5.1.0")
    
    // Audio Player für Radio Streaming
    implementation("com.sedmelluq:lavaplayer:1.3.78")
    
    // HTTP Client für Radio Browser API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // JSON Serialisierung
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}