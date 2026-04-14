plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
}
dependencies {
    implementation(project(":stealthx-crypto"))
    implementation(project(":shared"))
    // NO :data, NO :security, NO Android deps
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
}
tasks.withType<Test> { useJUnitPlatform() }
