plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}
android {
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    namespace = "com.stealthx.core"
    compileSdk = 37
    defaultConfig { minSdk = 26 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { aidl = true }
}
dependencies {
    implementation(project(":data"))
    implementation(project(":stealthx-crypto"))
    implementation(project(":security"))
    implementation(project(":shared"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testRuntimeOnly(libs.lazysodium.java)
}
// lazysodium-android 5.2.0 ships Java 21 bytecode and duplicates the lazysodium-java classes;
// JVM unit tests use lazysodium-java only (see presentation/build.gradle.kts).
configurations.matching { it.name.endsWith("UnitTestRuntimeClasspath") }.configureEach {
    exclude(group = "com.goterl", module = "lazysodium-android")
}

tasks.withType<Test> { useJUnitPlatform() }
