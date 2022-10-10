plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    val jacksonVersion = "2.13.2"

    compileOnly("net.kyori", "adventure-api", "4.11.0")

    implementation("org.apache.httpcomponents", "httpclient", "4.5.13")
    implementation("org.java-websocket", "Java-WebSocket", "1.5.2")
    implementation("com.fasterxml.jackson.core", "jackson-core", jacksonVersion)
    implementation("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml", jacksonVersion)
    implementation("com.fasterxml.jackson.module", "jackson-module-kotlin", jacksonVersion)
}
