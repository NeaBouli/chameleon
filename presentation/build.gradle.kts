plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.android.legacy.kapt)
    alias(libs.plugins.hilt)
}
android {
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    namespace = "com.stealthx.presentation"
    compileSdk = 37
    defaultConfig { minSdk = 26 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { compose = true }
}
dependencies {
    implementation(project(":data"))
    implementation(project(":domain"))
    implementation(project(":stealthx-crypto"))
    implementation(project(":core"))
    implementation(project(":features:overlay"))
    implementation(project(":features:messenger"))
    implementation(project(":features:privatezone"))
    implementation(project(":features:geofencing"))
    implementation(project(":features:decoy"))
    implementation(project(":stealthx-access"))
    implementation(project(":shared"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    implementation(libs.compose.navigation)
    implementation(libs.compose.lifecycle)
    implementation(libs.compose.hilt.navigation)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.biometric)
    implementation(libs.zxing.android)
    implementation(libs.okhttp)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    // lazysodium-android 5.2.0 ships Java 21 bytecode, which the JDK 17 unit-test VM cannot load
    // (MockK then fails to instrument ChameleonCrypto). Unit tests use the JVM build instead.
    testRuntimeOnly(libs.lazysodium.java)
    testRuntimeOnly(libs.jna)
}

configurations.matching { it.name.endsWith("UnitTestRuntimeClasspath") }.configureEach {
    exclude(group = "com.goterl", module = "lazysodium-android")
}

tasks.withType<Test> { useJUnitPlatform() }
