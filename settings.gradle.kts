pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Chameleon"

include(":app")
include(":stealthx-crypto")
include(":stealthx-ifr")
include(":security")
include(":core")
include(":data")
include(":domain")
include(":features:overlay")
include(":features:messenger")
include(":features:privatezone")
include(":features:geofencing")
include(":features:decoy")
include(":presentation")
include(":shared")
