plugins {
    kotlin("jvm") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.github.cinnaio"
version = "1.3.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    implementation(kotlin("stdlib-jdk8"))
    
    // Database connection pool
    implementation("com.zaxxer:HikariCP:5.1.0")
    
    // Log4j2 for silencing HikariCP
    compileOnly("org.apache.logging.log4j:log4j-core:2.17.1")
}

kotlin {
    jvmToolchain(21)
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
