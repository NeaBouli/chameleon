plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}
android {
    namespace = "com.stealthx.ifr"
    compileSdk = 35
    defaultConfig { minSdk = 26 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
dependencies {
    implementation(project(":stealthx-crypto"))
    implementation(project(":shared"))
    // Web3j LITE — read-only eth_call, no transactions
    implementation(libs.web3j.core)
    // TODO S-06: WalletConnect v2 — needs Reown Maven repo
    // implementation(libs.walletconnect.modal)
    // implementation(libs.walletconnect.sign)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.mockk)
}
tasks.withType<Test> { useJUnitPlatform() }
