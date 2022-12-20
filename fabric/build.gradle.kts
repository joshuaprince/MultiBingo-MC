@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)

    alias(libs.plugins.fabric.loom)
}

dependencies {
    implementation(project(":core"))

    minecraft("com.mojang", "minecraft", properties["minecraft_version"] as String)
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc", "fabric-loader", properties["loader_version"] as String)
    modImplementation("net.fabricmc.fabric-api", "fabric-api", properties["fabric_version"] as String)
    modImplementation("net.fabricmc", "fabric-language-kotlin", properties["fabric_kotlin_version"] as String)
    modImplementation(include("net.kyori", "adventure-platform-fabric", properties["adventure_fabric_version"] as String))
}

tasks {
    processResources {
        filesMatching("*.json") {
            expand(project.properties)
        }
    }
}
