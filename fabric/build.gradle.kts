@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gitversion)
    alias(libs.plugins.shadow)

    alias(libs.plugins.fabric.loom)
}

dependencies {
    implementation(project(":core"))

    // TODO: Define these somewhere externally
    // TODO: Link these in fabric.mod.json
    minecraft("com.mojang", "minecraft", "1.19.2")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc", "fabric-loader", "0.14.11")
    modImplementation("net.fabricmc.fabric-api", "fabric-api", "0.68.0+1.19.2")
    modImplementation("net.fabricmc", "fabric-language-kotlin", "1.8.6+kotlin.1.7.21")
    modImplementation(include("net.kyori:adventure-platform-fabric:5.5.1")!!)
}

tasks {
    processResources {
        filesMatching("*.mixins.json") { expand(mutableMapOf("java" to "17")) }
    }
}
