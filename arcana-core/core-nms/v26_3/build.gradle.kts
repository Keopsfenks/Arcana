import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("io.papermc.paperweight.userdev")
}

group = "com.keopsfenks"
version = rootProject.version

dependencies {
    implementation(project(":arcana-core:core-nms:v26_2", configuration = "shadow"))
    paperweight.paperDevBundle("26.3.build.+")
}

tasks {
    shadowJar {
        relocate(
            "com.keopsfenks.arcana.internal.spigot.proxy.v26_2",
            "com.keopsfenks.arcana.internal.spigot.proxy.v26_3"
        )

        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v26_2/CommonsInitializer*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v26_2/DatapackCodec*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v26_2/ItemComponents*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v26_2/Hologram*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v26_2/hologram/V26_2HologramHandle*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v26_2/common/ai/entity/CatLieOnBedGoalFactory*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v26_2/common/ai/entity/TryFindWaterGoalFactory*.class")

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    compileJava {
        options.release.set(25)
    }

    compileKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_25)
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}
