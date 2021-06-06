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
    val paperVersion = "1.16.5"
    val jacksonVersion = "2.12.3"

    shadow("com.destroystokyo.paper", "paper-api", "$paperVersion-R0.1-SNAPSHOT")

    implementation("org.apache.httpcomponents", "httpclient", "4.5.13")
    implementation("org.java-websocket", "Java-WebSocket", "1.5.2")
    implementation("dev.jorel.CommandAPI", "commandapi-shade", "5.12")
    implementation("com.fasterxml.jackson.core", "jackson-core", jacksonVersion)
    implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", jacksonVersion)
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", jacksonVersion)

    testImplementation("com.destroystokyo.paper", "paper-api", "$paperVersion-R0.1-SNAPSHOT")
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
