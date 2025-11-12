plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.vanniktech.maven.publish")
}

group = "dev.moshalan"
version = "2.1.0"

ktlint {
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.HTML)
    }
}

android {
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    pom {
        name.set("Easy-Analytics")
        description.set(
            "A powerful, annotation-based tracking library for Android that eliminates " +
                "boilerplate code by automatically injecting analytics tracking at " +
                "compile time using bytecode transformation.",
        )
        url.set("https://github.com/sh3lan93/analytics-annotation")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("shalan")
                name.set("Mohamed Shalan")
                email.set("mohamed.sh3lan.93@gmail.com")
            }
        }

        scm {
            connection.set("scm:git:git://github.com/sh3lan93/analytics-annotation")
            developerConnection.set("scm:git:ssh://github.com/sh3lan93/analytics-annotation")
            url.set("https://github.com/sh3lan93/analytics-annotation")
        }
    }
}