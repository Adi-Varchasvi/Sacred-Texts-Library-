plugins {
    id("com.android.application") version "8.7.3"
    id("org.jetbrains.kotlin.android") version "2.3.0"
}

android {
    namespace = "eu.kanade.tachiyomi.extension.en.sacredtextslibrary"
    compileSdk = 34

    defaultConfig {
        applicationId = "eu.kanade.tachiyomi.extension.en.sacredtextslibrary"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            // Release signing is wired to CI secrets (see README.md "Signing the
            // release APK"). When the secrets are absent the release build stays
            // unsigned so local/CI smoke builds still succeed.
            System.getenv("KEYSTORE_PATH")?.takeIf { it.isNotBlank() }?.let {
                storeFile = file(it)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (System.getenv("KEYSTORE_PATH")?.isNotBlank() == true) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    // keiyoushi v14 extensions-lib from JitPack. Provides the
    // eu.kanade.tachiyomi.* source API (HttpSource, models, network helpers).
    // The legacy tachiyomi.extension Gradle plugin and maven.tachiyomi.org
    // are dead, so this is used as a plain compileOnly dependency instead.
    // NOTE: keep Kotlin at 2.3.0 — the lib was compiled with Kotlin 2.3
    // metadata and other versions break API visibility.
    compileOnly("com.github.keiyoushi:extensions-lib:18a8e26be2")
    // okhttp3 is used directly (Headers, Request, Response) but the JitPack
    // POM does not expose it transitively, so it is declared explicitly.
    // compileOnly: the host app provides okhttp at runtime.
    compileOnly("com.squareup.okhttp3:okhttp:4.12.0")
}
