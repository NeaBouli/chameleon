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

    namespace = "com.stealthx.data"
    compileSdk = 37
    defaultConfig {
        minSdk = 26
        val entitlementKey = System.getenv("STEALTHX_ENTITLEMENT_PUBLIC_KEY_BASE64")
            ?: providers.gradleProperty("stealthxEntitlementPublicKeyBase64").orNull
            ?: ""
        require(entitlementKey.isEmpty() || entitlementKey.matches(Regex("^[A-Za-z0-9_-]{43}$"))) {
            "STEALTHX_ENTITLEMENT_PUBLIC_KEY_BASE64 must be an unpadded 32-byte base64url key"
        }
        buildConfigField("String", "ENTITLEMENT_PUBLIC_KEY_BASE64", "\"$entitlementKey\"")
    }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation(project(":domain"))
    implementation(project(":stealthx-crypto"))
    implementation(project(":security"))
    implementation(project(":shared"))
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.sqlcipher)
    implementation(libs.sqlite.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.datastore)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testRuntimeOnly(libs.lazysodium.java)
}
// lazysodium-android 5.2.0 ships Java 21 bytecode and duplicates the lazysodium-java classes;
// JVM unit tests use lazysodium-java only (see presentation/build.gradle.kts).
configurations.matching { it.name.endsWith("UnitTestRuntimeClasspath") }.configureEach {
    exclude(group = "com.goterl", module = "lazysodium-android")
}

tasks.withType<Test> { useJUnitPlatform() }

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}
