plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    id("org.jlleitschuh.gradle.ktlint")
    id("dev.moshalan.easyanalytics") version "2.0.0" // Analytics plugin for automatic screen tracking
}

ktlint {
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML)
    }
}

// Analytics plugin configuration
analytics {
    enabled = true

    // Include only our app packages for optimization
    includePackages = setOf("com.shalan.analyticsannotation")
    excludePackages = setOf("com.shalan.analyticsannotation.exclude")

    // Method tracking configuration
    methodTracking {
        enabled = true
        maxParametersPerMethod = 10
        validateAnnotations = true
        excludeMethods = setOf("toString", "hashCode", "equals")
    }
}

android {
    namespace = "com.shalan.analyticsannotation"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.shalan.analyticsannotation"

        targetSdk = 34
        minSdk = 26
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments.putAll(mapOf("clearPackageData" to "true"))
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    dependencies {

        implementation(project(":annotation"))
        implementation(project(":core"))
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.appcompat)
        implementation(libs.material)
        implementation(libs.androidx.ui)
        implementation(libs.androidx.ui.graphics)
        implementation(libs.androidx.ui.tooling.preview)
        implementation(libs.androidx.material3)
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(libs.androidx.runner)
        androidTestImplementation(libs.androidx.rules)
        androidTestImplementation(libs.androidx.junit.v121)
        androidTestImplementation(libs.androidx.espresso.intents)
        androidTestImplementation(libs.androidx.orchestrator)
        androidTestImplementation(libs.truth)
        androidTestImplementation(libs.androidx.core)
        androidTestImplementation(libs.androidx.fragment.ktx)
        androidTestImplementation(libs.androidx.fragment.testing)
        debugImplementation(libs.androidx.espresso.idling.resource)
        debugImplementation(libs.androidx.ui.tooling)
        debugImplementation(libs.androidx.ui.test.manifest)
    }
}
