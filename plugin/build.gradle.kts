plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("org.jlleitschuh.gradle.ktlint")
}

tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    plugins {
        create("analyticsPlugin") {
            id = "com.shalan.analytics"
            implementationClass = "com.shalan.analytics.plugin.AnalyticsPlugin"
            displayName = "Analytics Screen Tracking Plugin"
            description = "Automatically injects screen tracking code into Android Activities, Fragments, and Composables"
        }
    }
}

dependencies {
    implementation(gradleApi())
    implementation("com.android.tools.build:gradle:8.7.3")
    implementation("org.ow2.asm:asm:9.7")
    implementation("org.ow2.asm:asm-commons:9.7")
    implementation("org.ow2.asm:asm-util:9.7")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.14.2")
}
