plugins {
    id("java")
    id("com.gradleup.shadow") version "9.3.1"
}

group = "net.azisaba"
version = "1.0-SNAPSHOT"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/public/")
    maven("https://repo.azisaba.net/repository/maven-public/")
}

dependencies {
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.7")
    implementation("com.zaxxer:HikariCP:4.0.3")
    compileOnly("net.azisaba.azipluginmessaging:api:4.0.4")
    compileOnly("org.spigotmc:spigot-api:1.12.2-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:26.0.2-1")
}

tasks {
    shadowJar {
        enableAutoRelocation.set(true)
        relocationPrefix.set("net.azisaba.goldencloth.libs")
    }
}
