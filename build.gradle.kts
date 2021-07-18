import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.21"
    id("com.github.johnrengelman.shadow") version "7.0.0"
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
            val kotlinVersion = "1.5.21"
            val koinVersion = "3.1.2"
            implementation(kotlin("stdlib", kotlinVersion))
            implementation(kotlin("stdlib-jdk7", kotlinVersion))
            implementation(kotlin("reflect", kotlinVersion))
            implementation("io.insert-koin", "koin-core", koinVersion)
            testImplementation(kotlin("test-junit5"))
            testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
            testImplementation("io.insert-koin", "koin-test", koinVersion)
            testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
        }

        tasks.withType<KotlinCompile> {
            kotlinOptions.jvmTarget = "16"
        }

        tasks.withType<ShadowJar> {
            // Can re-enable minimize when fixed: https://github.com/johnrengelman/shadow/issues/679
            /*
            minimize {
                // https://thelyfsoshort.io/kotlin-reflection-shadow-jars-minimize-9bd74964c74
                exclude(dependency("org.jetbrains.kotlin:.*"))
            }
            */
        }
    }
}
