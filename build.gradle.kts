// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.3.0"
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        additionalEditorconfig.set(
            mapOf(
                "ktlint_function-naming" to "disabled",
                "ktlint_standard_function-naming" to "disabled",
                "ktlint_standard_comment-wrapping" to "disabled",
            ),
        )
        filter {
            exclude("**/generated/**")
        }
    }
}
