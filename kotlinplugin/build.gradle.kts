import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.5.0"
    id("com.github.johnrengelman.shadow") version "6.1.0"
    id("com.palantir.git-version") version "0.12.3"
}

val gitVersion: groovy.lang.Closure<String> by extra

group = "com.jtprince.bingo.kplugin"
version = gitVersion()

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    val kotlinVersion = "1.5.0"
    val paperVersion = "1.16.5"
    val ktorVersion = "1.5.4"
    val jacksonVersion = "2.12.3"

    shadow("com.destroystokyo.paper", "paper-api", "$paperVersion-R0.1-SNAPSHOT")

    implementation(kotlin("stdlib", kotlinVersion))
    implementation(kotlin("reflect", kotlinVersion))
    implementation("io.ktor", "ktor-client-core", ktorVersion)
    implementation("io.ktor", "ktor-client-websockets", ktorVersion)
    implementation("io.ktor", "ktor-client-cio", ktorVersion)
    implementation("io.ktor", "ktor-client-jackson", ktorVersion)
    implementation("dev.jorel.CommandAPI", "commandapi-shade", "5.12")
    implementation("com.fasterxml.jackson.core", "jackson-core", jacksonVersion)
    implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", jacksonVersion)
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", jacksonVersion)

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testImplementation("com.destroystokyo.paper", "paper-api", "$paperVersion-R0.1-SNAPSHOT")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.processResources {
    inputs.property("gitVersion", version)  // Fix caching in plugin.yml
    filesMatching("plugin.yml") {
      expand(project.properties)
    }
}

val dumpAutomatedGoals = task("dumpAutomatedGoals", JavaExec::class) {
    val outfile = "automated_goals.txt"
    inputs.dir("src/main/kotlin/com/jtprince/bingo/kplugin/automark")
    inputs.dir("src/main/resources")
    outputs.file(outfile)

    main = "com.jtprince.bingo.kplugin.automark.AutomatedGoalList"
    classpath = sourceSets.main.get().runtimeClasspath + sourceSets.main.get().compileClasspath
    args(outfile)
}

tasks {
    assemble {
        dependsOn.add(project.tasks.shadowJar)
        dependsOn.add(dumpAutomatedGoals)
    }

    named<ShadowJar>("shadowJar") {
        archiveFileName.set("MultiBingo.jar")
    }
}
