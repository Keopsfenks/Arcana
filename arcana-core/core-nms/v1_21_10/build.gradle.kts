plugins {
    id("io.papermc.paperweight.userdev")
}

group = "com.keopsfenks"
version = rootProject.version

dependencies {
    implementation(project(":arcana-core:core-nms:common"))
    implementation(project(":arcana-core:core-nms:v1_21_8", configuration = "shadow"))
    paperweight.paperDevBundle("1.21.10-R0.1-SNAPSHOT")
}

tasks {
    build {
        dependsOn(reobfJar)
    }

    reobfJar {
        mustRunAfter(shadowJar)
    }

    shadowJar {
        relocate(
            "com.keopsfenks.arcana.internal.spigot.proxy.v1_21_8",
            "com.keopsfenks.arcana.internal.spigot.proxy.v1_21_10"
        )
        relocate(
            "com.keopsfenks.arcana.internal.spigot.proxy.common",
            "com.keopsfenks.arcana.internal.spigot.proxy.v1_21_10.common"
        )

        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v1_21_8/TPS*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v1_21_8/ItemComponents*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v1_21_8/Hologram*.class")
        // Superseded by this version's own handle, and no longer links here.
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v1_21_8/common/hologram/CommonHologramHandle*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/common/hologram/CommonHologramHandle*.class")

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
