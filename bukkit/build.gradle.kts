import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

group = "com.jtprince.bingo.bukkit"

repositories {
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    implementation(project(":core"))

    val paperVersion = "1.19.2"
    val jacksonVersion = "2.13.2"

    shadow("io.papermc.paper", "paper-api", "$paperVersion-R0.1-SNAPSHOT")

    implementation("org.apache.httpcomponents", "httpclient", "4.5.13")  // TODO remove
    implementation("org.java-websocket", "Java-WebSocket", "1.5.2")  // TODO remove
    implementation("dev.jorel.CommandAPI", "commandapi-shade", "8.5.1")
    implementation("com.fasterxml.jackson.core", "jackson-core", jacksonVersion)  // TODO remove
    implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", jacksonVersion)  // TODO remove
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", jacksonVersion)  // TODO remove

    testImplementation("io.papermc.paper", "paper-api", "$paperVersion-R0.1-SNAPSHOT")
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    inputs.property("gitVersion", version)  // Fix caching in plugin.yml
    filesMatching("plugin.yml") {
      expand(project.properties)
    }
}

val dumpAutomatedGoals = task("dumpAutomatedGoals", JavaExec::class) {
    val outfile = "automated_goals.txt"
    inputs.dir("src/main/kotlin/com/jtprince/bingo/bukkit/automark/definitions")
    inputs.dir("src/main/resources")
    outputs.file(outfile)

    main = "com.jtprince.bingo.bukkit.automark.definitions.AutomatedGoalList"
    classpath = sourceSets.main.get().runtimeClasspath + sourceSets.main.get().compileClasspath
    args(outfile)
}

tasks {
    assemble {
        dependsOn.add(project.tasks.shadowJar)
        dependsOn.add(dumpAutomatedGoals)
    }

    withType<ShadowJar> {
        archiveFileName.set("MultiBingo-Bukkit.jar")
    }
}
