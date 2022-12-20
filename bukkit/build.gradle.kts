@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gitversion)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":core"))

    shadow(libs.paper.api)
}
