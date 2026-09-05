pluginManagement {
    repositories {
        google { content { includeGroupByRegex("com\\.android.*"); includeGroupByRegex("com\\.google.*"); includeGroupByRegex("androidx.*") } }
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
rootProject.name = "AutoDrive"
include(":app")
include(":core:model", ":core:common", ":core:database", ":core:network")
include(":core:observability", ":core:session", ":core:sync", ":core:designsystem", ":core:platform")
include(":feature:auth", ":feature:chat", ":feature:notifications")
include(":feature:commission", ":feature:balance", ":feature:profile", ":feature:achievements")
