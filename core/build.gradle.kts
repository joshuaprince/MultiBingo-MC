plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    compileOnly("net.kyori", "adventure-api", "4.7.0")
}
