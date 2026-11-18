pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "One Hand Keyboard"

// Main app module (renamed to app-ime for clarity)
include(":app-ime")
project(":app-ime").projectDir = file("app")

// Core modules
include(":core:gesture")
include(":core:mapping")
include(":core:design")
include(":core:domain")
