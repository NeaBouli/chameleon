plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}
android {
    namespace = "com.stealthx.presentation"
    compileSdk = 35
    defaultConfig { minSdk = 26 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
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
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
}

tasks.withType<Test> { useJUnitPlatform() }
