plugins {
    id("android-library-convention")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.shalan.analytics.core"
}

dependencies {
    api(project(":annotation"))
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.common.java8)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.annotation)

    // Testing dependencies
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.junit.v121)
    testImplementation(libs.robolectric)
}

mavenPublishing {
    coordinates(group.toString(), "easy-analytics-core", version.toString())
}
