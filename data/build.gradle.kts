plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}
android {
    namespace = "com.stealthx.data"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
                arguments["room.incremental"]    = "true"
            }
        }
    }
}
dependencies {
    implementation(project(":domain"))
    implementation(project(":stealthx-crypto"))
    implementation(project(":shared"))
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
    implementation(libs.sqlcipher)
    implementation(libs.sqlite.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.datastore)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockk)
}
tasks.withType<Test> { useJUnitPlatform() }
