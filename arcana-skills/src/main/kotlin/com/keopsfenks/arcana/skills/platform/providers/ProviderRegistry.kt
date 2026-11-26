package com.keopsfenks.arcana.skills.platform.providers

import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.DefaultSkillPlatform
import com.keopsfenks.arcana.skills.platform.registry.PlatformRegistration

/** Provider views reuse the platform's typed registries and shared owner gate. */
class ProviderRegistry(private val platform: DefaultSkillPlatform) {
    fun resource(name: String, namespace: String, path: String) = resolve(SkillComponentType.RESOURCE, name, namespace, path)
    fun stat(name: String, namespace: String, path: String) = resolve(SkillComponentType.STAT, name, namespace, path)
    private fun <T : SkillPlatformComponent> resolve(type: SkillComponentType<T>, name: String, namespace: String, path: String): PlatformRegistration<T> {
        val key = SkillKey.parse(name, namespace)
        return platform.registry(type).get(key) ?: (if (':' !in name) platform.registry(type).get(SkillKey.parse(name, "arcana")) else null)
            ?: throw IllegalArgumentException("$path: unknown ${type.id()} provider $key")
    }
}
