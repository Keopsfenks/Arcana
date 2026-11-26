package com.keopsfenks.arcana.internal.spigot.skills
import com.keopsfenks.arcana.internal.spigot.ArcanaImpl
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PlatformDistributionTests {
    @Test fun productionStartupIncludesCatalogAndSeparateExampleCopierAndResources() {
        val bytes = requireNotNull(ArcanaImpl::class.java.getResourceAsStream("ArcanaImpl.class")).readBytes().toString(Charsets.ISO_8859_1)
        assertTrue(bytes.contains("StandardSkillCatalog")); assertTrue(bytes.contains("StandardSkillExamples"))
        val schema = requireNotNull(javaClass.getResourceAsStream("/arcana-skills/schema-v2.json")).bufferedReader().readText()
        assertTrue(schema.contains("arcana:projectile")); assertTrue(schema.contains("arcana:has_aura"))
        val example = requireNotNull(javaClass.getResourceAsStream("/arcana-skills/examples/v2/general-events.yml")).bufferedReader().readText()
        assertTrue(example.contains("enabled: false")); assertTrue(example.contains("schema: 2"))
    }
}
