pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "jetbrains-llm-pycharm-plugin"
include("src:main:test")
findProject(":src:main:test")?.name = "test"
