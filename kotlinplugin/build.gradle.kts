import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.4.32"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("com.palantir.git-version") version "0.12.3"
}

val gitVersion: groovy.lang.Closure<String> by extra

group = "com.jtprince.bingo.kplugin"
version = gitVersion()

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://raw.githubusercontent.com/JorelAli/CommandAPI/mvn-repo/")
}

dependencies {
    shadow("com.destroystokyo.paper", "paper-api", "1.16.5-R0.1-SNAPSHOT")

    implementation(kotlin("stdlib"))
    implementation("io.ktor", "ktor-client-core", "1.5.3")
    implementation("io.ktor", "ktor-client-websockets", "1.5.3")
    implementation("io.ktor", "ktor-client-cio", "1.5.3")
    implementation("io.ktor", "ktor-client-jackson", "1.5.3")
    implementation("dev.jorel", "commandapi-shade", "5.9")
    implementation("com.fasterxml.jackson.core", "jackson-core", "2.12.2")
    implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", "2.12.2")
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", "2.12.2")

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testImplementation("com.destroystokyo.paper", "paper-api", "1.16.5-R0.1-SNAPSHOT")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.processResources {
    inputs.property("gitVersion", version)  // Fix caching in plugin.yml
    filesMatching("plugin.yml") {
      expand(project.properties)
    }
}

tasks {
    assemble {
        dependsOn.add(project.tasks.shadowJar)
        dependsOn.add(project.tasks.shadowJar)
    }

    named<ShadowJar>("shadowJar") {
        archiveFileName.set("MultiBingo.jar")
    }
}
