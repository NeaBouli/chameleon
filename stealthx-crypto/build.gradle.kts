/*
 * :stealthx-crypto — THE ONLY CRYPTO MODULE
 * ==========================================
 * All cryptographic operations live here.
 * NO other module may use lazysodium directly.
 * NO other module may implement crypto logic.
 *
 * Dependencies: :shared ONLY
 */
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":shared"))

    // THE ONLY CRYPTO LIBRARY — lazysodium wraps libsodium
    // XChaCha20-Poly1305, X25519, Double Ratchet, Argon2id, Ed25519
    implementation(libs.lazysodium.android)
    implementation(libs.jna)

    implementation(libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
