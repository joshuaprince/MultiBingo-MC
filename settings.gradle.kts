rootProject.name = "MultiBingo-MC"
include("core", "bukkit", "fabric")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net") { name = "Fabric" }
        maven("https://papermc.io/repo/repository/maven-public/")
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net") { name = "Fabric" }
        maven("https://papermc.io/repo/repository/maven-public/")
    }

    versionCatalogs {
        create("libs") {
            plugin("fabric-loom", "fabric-loom").version("1.0-SNAPSHOT")
            plugin("gitversion", "com.palantir.git-version").version("0.15.0")
            plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm").version("1.7.21")
            plugin("shadow", "com.github.johnrengelman.shadow").version("7.1.2")

            library("adventure-api", "net.kyori", "adventure-api").version("4.12.0")
//            library("adventure-fabric", "net.kyori", "adventure-platform-fabric").version("5.6.0")
            library("jackson-core", "com.fasterxml.jackson.core", "jackson-core").version("2.14.1")
            library("jackson-kotlin", "com.fasterxml.jackson.module", "jackson-module-kotlin").version("2.14.1")
            library("jackson-yaml", "com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml").version("2.14.1")
            library("kotlinx-coroutines", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").version("1.6.4")
            library("okhttp", "com.squareup.okhttp3", "okhttp").version("5.0.0-alpha.10")
            library("paper-api", "io.papermc.paper", "paper-api").version("1.19.2-R0.1-SNAPSHOT")
            library("websocket", "org.java-websocket", "Java-WebSocket").version("1.5.3")
        }
    }
}
