pluginManagement {
    repositories {
        maven("https://repo.papermc.io/repository/maven-public/")
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "arcana"

// Core
include(":arcana-api")
include(":arcana-core")
include(":arcana-core:core-nms")
include(":arcana-core:core-nms:common")
include(":arcana-core:core-nms:v1_21_8")
include(":arcana-core:core-nms:v1_21_10")
include(":arcana-core:core-nms:v1_21_11")
include(":arcana-core:core-nms:v26_1_2")
include(":arcana-core:core-nms:v26_2")
include(":arcana-core:core-nms:v26_3")
include(":arcana-core:core-plugin")
include(":arcana-core:core-backend")
