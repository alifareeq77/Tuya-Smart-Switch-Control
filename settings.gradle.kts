pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // TODO: Add Tuya/Thingclips maven repo if your account requires a private repo URL.
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // TODO: Add Tuya/Thingclips maven repo if required by your SDK version.
    }
}

rootProject.name = "TuyaSmartSwitchControl"
include(":app")
