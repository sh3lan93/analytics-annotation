plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "2.0.0"
    id("org.jlleitschuh.gradle.ktlint")
}

group = "dev.moshalan"
version = "2.1.0"

tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    website = "https://github.com/sh3lan93/analytics-annotation"
    vcsUrl = "https://github.com/sh3lan93/analytics-annotation"

    plugins {
        create("easyAnalyticsPlugin") {
            id = "dev.moshalan.easyanalytics"
            implementationClass = "com.shalan.analytics.plugin.AnalyticsPlugin"
            displayName = "Easy Analytics Plugin"
            description = "Automatically injects analytics tracking code into Android Activities and Fragments " +
                "at compile time using bytecode transformation. Supports screen-level tracking and method-level " +
                "event tracking with parameter serialization, all with zero boilerplate."
            tags = listOf("android", "analytics", "tracking", "bytecode", "annotation")
        }
    }
}

dependencies {
    implementation(gradleApi())
    implementation("com.android.tools.build:gradle:8.7.3")
    implementation("com.android.tools.build:gradle-api:8.7.3")
    implementation("org.ow2.asm:asm:9.7")
    implementation("org.ow2.asm:asm-commons:9.7")
    implementation("org.ow2.asm:asm-util:9.7")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation(kotlin("test"))
}
