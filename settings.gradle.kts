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
        // extensions-lib (keiyoushi v14) is published here; see extension/build.gradle.kts
        maven("https://jitpack.io")
    }
}

rootProject.name = "sacred-texts-library-extension"

include(":extension")
