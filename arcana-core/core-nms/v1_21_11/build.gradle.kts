plugins {
    id("io.papermc.paperweight.userdev")
}

group = "com.keopsfenks"
version = rootProject.version

dependencies {
    implementation(project(":arcana-core:core-nms:common"))
    implementation(project(":arcana-core:core-nms:v1_21_8", configuration = "shadow"))
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
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
            "com.keopsfenks.arcana.internal.spigot.proxy.v1_21_11"
        )
        relocate(
            "com.keopsfenks.arcana.internal.spigot.proxy.common",
            "com.keopsfenks.arcana.internal.spigot.proxy.v1_21_11.common"
        )

        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v1_21_8/PlayerHandler*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v1_21_8/TPS*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v1_21_8/ItemComponents*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v1_21_8/ItemUse*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v1_21_8/CommonsInitializer*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v1_21_8/SNBTConverter*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v1_21_8/SpiderClimbing*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v1_21_8/EnchantmentRegistry*.class")

        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v1_21_8/Hologram*.class")
        // Superseded by this version's own handle, and no longer links here.
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v1_21_8/common/hologram/CommonHologramHandle*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/common/hologram/CommonHologramHandle*.class")

        // 1.21.11 moved the entity classes these goals use.
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v1_21_8/common/ai/entity/CatLieOnBedGoalFactory*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v1_21_8/common/ai/entity/CatSitOnBedGoalFactory*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v1_21_8/common/ai/entity/IllusionerBlindnessSpellGoalFactory*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v1_21_8/common/ai/entity/IllusionerMirrorSpellGoalFactory*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/v1_21_8/common/ai/target/DefendVillageGoalFactory*.class")

        exclude("com/keopsfenks/arcana/internal/spigot/proxy/common/ai/entity/CatLieOnBedGoalFactory*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/common/ai/entity/CatSitOnBedGoalFactory*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/common/ai/entity/IllusionerBlindnessSpellGoalFactory*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/common/ai/entity/IllusionerMirrorSpellGoalFactory*.class")
        exclude("com/keopsfenks/arcana/internal/spigot/proxy/common/ai/target/DefendVillageGoalFactory*.class")

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
