plugins {
    alias(libs.plugins.kotlin.jvm)
}
dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
}
tasks.withType<Test> { useJUnitPlatform() }
