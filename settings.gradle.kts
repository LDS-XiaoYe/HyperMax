@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        // Mirrors first: several artifacts on google()/mavenCentral() are redirected
        // to github.com release assets, which is unreachable here and stalls the build.
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        google()
        mavenCentral()
        mavenLocal()
        maven("https://jitpack.io")
        // miuix artifacts are published here; public, no credentials needed
        maven("https://maven.pkg.github.com/ReChronoRain/HyperCeiler")
        maven("https://api.xposed.info")
    }
}

rootProject.name = "HyperMax"

include(
    "app",
    // ":library:hook",
    ":library:libhook",
    // ":library:xposed-api-101",
    ":library:core",
    ":library:provision",
    ":library:common",
    ":library:processor",
    ":library:hidden-api",
)
