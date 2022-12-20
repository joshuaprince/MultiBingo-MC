rootProject.name = "MultiBingo-MC"
include("core", "fabric")

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
            plugin("fabric-loom", "fabric-loom").version(extra.properties["loom_version"] as String)
            plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm").version(extra.properties["kotlin_version"] as String)
            plugin("shadow", "com.github.johnrengelman.shadow").version(extra.properties["shadow_version"] as String)

            library("adventure-api", "net.kyori", "adventure-api").version(extra.properties["adventure_api_version"] as String)
//            library("adventure-fabric", "net.kyori", "adventure-platform-fabric").version(extra.properties["adventure_fabric_version"] as String)
            library("jackson-core", "com.fasterxml.jackson.core", "jackson-core").version(extra.properties["jackson_version"] as String)
            library("jackson-kotlin", "com.fasterxml.jackson.module", "jackson-module-kotlin").version(extra.properties["jackson_version"] as String)
            library("jackson-yaml", "com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml").version(extra.properties["jackson_version"] as String)
            library("kotlinx-coroutines", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").version(extra.properties["kotlinx_version"] as String)
            library("okhttp", "com.squareup.okhttp3", "okhttp").version(extra.properties["okhttp_version"] as String)
            library("paper-api", "io.papermc.paper", "paper-api").version(extra.properties["paper_api_version"] as String)
            library("websocket", "org.java-websocket", "Java-WebSocket").version(extra.properties["websocket_version"] as String)
        }
    }
}
