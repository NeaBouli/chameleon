/*
 * Chameleon — :app module
 * Entry point. DI graph root. No business logic here.
 */
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

val localProps = Properties().also { props ->
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { props.load(it) }
}

android {
    namespace = "com.stealthx.chameleon"
    compileSdk = 35

    signingConfigs {
        create("release") {
            val ksPath = localProps["KEYSTORE_PATH"] as? String
            val ksPass = localProps["KEYSTORE_PASS"] as? String
            val ksAlias = localProps["KEY_ALIAS"] as? String ?: "chameleon"
            if (ksPath != null && ksPass != null) {
                storeFile = rootProject.file(ksPath)
                storePassword = ksPass
                keyAlias = ksAlias
                keyPassword = ksPass  // PKCS12: store password == key password
            }
        }
    }

    defaultConfig {
        applicationId = "chameleon24.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 10
        versionName = "0.1.9-alpha"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "FORCED_TIER", "\"\"")
        buildConfigField("Boolean", "ALLOW_SCREENSHOTS", "false")
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            isMinifyEnabled = false
            buildConfigField("Boolean", "FORCE_ELITE", "true")
            buildConfigField("String", "FORCED_TIER", "\"ELITE\"")
        }
        create("storeScreenshot") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".screenshots"
            versionNameSuffix = "-screenshots"
            buildConfigField("Boolean", "ALLOW_SCREENSHOTS", "true")
            buildConfigField("Boolean", "FORCE_ELITE", "true")
            buildConfigField("String", "FORCED_TIER", "\"ELITE\"")
            matchingFallbacks += listOf("debug")
        }
        create("internalRelease") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("release")
            applicationIdSuffix = ".internal"
            versionNameSuffix = "-internal"
            buildConfigField("Boolean", "FORCE_ELITE", "true")
            buildConfigField("String", "FORCED_TIER", "\"ELITE\"")
            matchingFallbacks += listOf("release")
        }
        create("freeTierRelease") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("release")
            applicationIdSuffix = ".free"
            versionNameSuffix = "-free"
            buildConfigField("Boolean", "FORCE_ELITE", "false")
            buildConfigField("String", "FORCED_TIER", "\"FREE\"")
            matchingFallbacks += listOf("release")
        }
        create("proTierRelease") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("release")
            applicationIdSuffix = ".pro"
            versionNameSuffix = "-pro"
            buildConfigField("Boolean", "FORCE_ELITE", "false")
            buildConfigField("String", "FORCED_TIER", "\"PRO\"")
            matchingFallbacks += listOf("release")
        }
        create("eliteTierRelease") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("release")
            applicationIdSuffix = ".elite"
            versionNameSuffix = "-elite"
            buildConfigField("Boolean", "FORCE_ELITE", "true")
            buildConfigField("String", "FORCED_TIER", "\"ELITE\"")
            matchingFallbacks += listOf("release")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("Boolean", "FORCE_ELITE", "false")
            buildConfigField("String", "FORCED_TIER", "\"\"")
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
        buildConfig = true
    }

    packaging {
        jniLibs {
            // JNA needs libsodium.so extracted to disk on older devices
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/DISCLAIMER"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/INDEX.LIST"
        }
    }

    // ABI filters — support 32+64 bit ARM and x86_64 for emulators
    defaultConfig {
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64")
        }
    }
}

dependencies {
    implementation(libs.androidx.work)
    implementation(project(":stealthx-crypto"))
    implementation(project(":stealthx-access"))
    implementation(project(":security"))
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(project(":features:overlay"))
    implementation(project(":features:messenger"))
    implementation(project(":features:privatezone"))
    implementation(project(":features:geofencing"))
    implementation(project(":features:decoy"))
    implementation(project(":presentation"))
    implementation(project(":shared"))

    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.compose.activity)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.navigation)
    implementation(libs.room.runtime)

    implementation(libs.timber)
}
