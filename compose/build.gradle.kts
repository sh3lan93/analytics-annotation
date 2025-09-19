plugins {
    id("android-compose-convention")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.shalan.analytics.compose"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":annotation"))

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.ui.tooling.preview)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.rules)

    testImplementation(libs.junit)

    // Use consistent AndroidX test versions
    androidTestImplementation(libs.androidx.core)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.androidx.rules)
    androidTestImplementation(libs.androidx.junit.v121)
    androidTestImplementation(libs.androidx.espresso.core)

    // Add missing core-ktx for test
    androidTestImplementation(libs.core.ktx)

    // Compose test dependencies
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.test.manifest)

    // MockK and other test utilities
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.kotlin.reflect.v2021)
    androidTestImplementation(libs.androidx.uiautomator)
}

mavenPublishing {
    coordinates(group.toString(), "easy-analytics-compose", version.toString())
}
