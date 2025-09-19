plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
    implementation("org.jlleitschuh.gradle:ktlint-gradle:12.3.0")
    implementation("com.vanniktech:gradle-maven-publish-plugin:0.34.0")
}