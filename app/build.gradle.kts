import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    kotlin("kapt")
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
}

// Short hash of the commit being built, shown on the keyboard to tell installed builds apart.
val gitCommit: String =
    runCatching {
        providers
            .exec {
                commandLine("git", "rev-parse", "--short", "HEAD")
                isIgnoreExitValue = true
            }.standardOutput.asText
            .get()
            .trim()
    }.getOrNull().orEmpty().ifEmpty { "dev" }

// Commit date (epoch seconds): the in-app updater only moves forward, never to an older build.
val gitCommitTime: Long =
    runCatching {
        providers
            .exec {
                commandLine("git", "log", "-1", "--format=%ct")
                isIgnoreExitValue = true
            }.standardOutput.asText
            .get()
            .trim()
            .toLong()
    }.getOrDefault(0L)

// Fixed key for published builds, so every update installs over the previous one (LAN or GitHub).
// Local builds read keystore.properties (not in git); the release workflow passes OHK_* variables.
// Without either, builds fall back to the usual per-machine debug key.
val signingProperties =
    Properties().apply {
        rootProject.file("keystore.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
    }

fun signingValue(
    property: String,
    environment: String,
): String? = signingProperties.getProperty(property) ?: providers.environmentVariable(environment).orNull

val sharedStoreFile = signingValue("storeFile", "OHK_KEYSTORE_FILE")

android {
    namespace = "com.onehandkeyboard"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.onehandkeyboard"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "GIT_COMMIT", "\"$gitCommit\"")
        buildConfigField("long", "GIT_COMMIT_TIME", "${gitCommitTime}L")
        // Latest GitHub release (published by .github/workflows/release.yml); no API, so no rate limit.
        buildConfigField(
            "String",
            "UPDATE_RELEASES_URL",
            "\"https://github.com/02900/Android-One-Hand-Keyboard/releases/latest/download\"",
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (sharedStoreFile != null) {
            create("shared") {
                storeFile = file(sharedStoreFile)
                storePassword = signingValue("storePassword", "OHK_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "OHK_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "OHK_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            signingConfigs.findByName("shared")?.let { signingConfig = it }
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Updates itself from tools/serve_updates.py on the LAN (permissions in src/debug).
            buildConfigField("boolean", "SELF_UPDATE", "true")
            // Has Internet anyway: also shows Know Your Meme's trending tab.
            buildConfigField("boolean", "TRENDING_MEMES", "true")
        }
        release {
            // Same fixed key as debug when available; otherwise Gradle leaves the release APK unsigned.
            signingConfigs.findByName("shared")?.let { signingConfig = it }
            buildConfigField("boolean", "SELF_UPDATE", "false")
            buildConfigField("boolean", "TRENDING_MEMES", "false")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        // Like release, plus Internet for Know Your Meme's trending tab (permission in src/online).
        // A separate app (.online), so it installs next to the offline one.
        create("online") {
            initWith(getByName("release"))
            applicationIdSuffix = ".online"
            versionNameSuffix = "-online"
            buildConfigField("boolean", "TRENDING_MEMES", "true")
            matchingFallbacks += "release"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    // Robolectric loads the real resources (e.g. the key icons drawn by KeyboardView).
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Feature modules

    // Core modules
    implementation(project(":core:domain"))
    implementation(project(":core:gesture"))
    implementation(project(":core:design"))

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.robolectric)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

kapt {
    correctErrorTypes = true
}
