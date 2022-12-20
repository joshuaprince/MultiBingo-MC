@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gitversion)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.adventure.api)

    implementation(libs.jackson.core)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.yaml)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.okhttp)
    implementation(libs.websocket)
}
