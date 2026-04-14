plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}
android {
    namespace = "com.stealthx.core"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    buildFeatures { aidl = true }
}
dependencies {
    implementation(project(":stealthx-crypto"))
    implementation(project(":security"))
    implementation(project(":shared"))
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
}
tasks.withType<Test> { useJUnitPlatform() }
