pluginManagement {
    repositories {
        google(); mavenCentral(); gradlePluginPortal()
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/central")
    }
}
dependencyResolutionManagement {
    repositories {
        google(); mavenCentral()
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/public")
    }
}
rootProject.name = "BCRGui"
include(":app")
