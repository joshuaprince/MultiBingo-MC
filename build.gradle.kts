import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("com.palantir.git-version") version "0.12.3"
}

val gitVersion: groovy.lang.Closure<String> by extra

allprojects {
    group = "com.jtprince.bingo"
    version = gitVersion()

    repositories {
        mavenCentral()
    }

    plugins.withType<KotlinPluginWrapper>().whenObjectAdded {
        dependencies {
            val kotlinVersion = "1.5.10"
            implementation(kotlin("stdlib", kotlinVersion))
            implementation(kotlin("stdlib-jdk7", kotlinVersion))
            implementation(kotlin("reflect", kotlinVersion))
            implementation("io.insert-koin", "koin-core", "3.0.2")
            testImplementation(kotlin("test-junit5"))
            testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
            testImplementation("io.insert-koin", "koin-test", "3.0.2")
            testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
        }

        tasks.withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "1.8"
        }

        tasks.withType<ShadowJar> {
            minimize {
                // https://thelyfsoshort.io/kotlin-reflection-shadow-jars-minimize-9bd74964c74
                exclude(dependency("org.jetbrains.kotlin:.*"))
            }
        }
    }
}

//configure(listOf(":bukkit")) {
//}

//dependencies {
//    module(":bukkit")
//}

//artifacts {
//    add("MultiBingoJarPlugin", project(":bukkit").tasks.assemble)
//}
