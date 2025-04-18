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
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "AndroidSamples"
include(":app")
include(":sample-di-hilt")
include(":sample-route-navigation")
include(":sample-preference")
include(":sample-nav-compose")
