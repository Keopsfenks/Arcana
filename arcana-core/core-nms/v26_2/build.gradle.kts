import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("io.papermc.paperweight.userdev")
}

group = "com.keopsfenks"
version = rootProject.version

dependencies {
    implementation(project(":arcana-core:core-nms:v26_1_2", configuration = "shadow"))
    paperweight.paperDevBundle("26.2.build.+")
}

tasks {
    shadowJar {
        relocate(
            "com.keopsfenks.arcana.internal.spigot.proxy.v26_1_2",
            "com.keopsfenks.arcana.internal.spigot.proxy.v26_2"
        )

        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v26_1_2/common/recipes/RecipeManager*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v26_1_2/SNBTConverter*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v26_1_2/ItemComponents*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v26_1_2/ItemUse*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v26_1_2/PlayerHandler*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v26_1_2/Hologram*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v26_1_2/hologram/V26_1_2HologramHandle*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v26_1_2/entity/ArcanaEntityController*.class")

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
