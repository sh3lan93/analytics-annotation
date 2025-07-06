plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.3.0"
    id("org.jlleitschuh.gradle.ktlint")
}

group = "com.shalan.analytics"
version = "1.0.0-SNAPSHOT"

tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    website = "https://github.com/shalan/analytics-annotation"
    vcsUrl = "https://github.com/shalan/analytics-annotation.git"

    plugins {
        create("analyticsPlugin") {
            id = "com.shalan.analytics"
            implementationClass = "com.shalan.analytics.plugin.AnalyticsPlugin"
            displayName = "Analytics Screen Tracking Plugin"
            description = "Automatically injects screen tracking code into Android Activities, Fragments, " +
                "and Composables at compile time using bytecode manipulation. Supports Activities, " +
                "Fragments, and Jetpack Compose with zero boilerplate."
            tags = listOf("android", "analytics", "tracking", "bytecode", "annotation", "compose")
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
