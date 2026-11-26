package com.keopsfenks.arcana.skills.platform.execution
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.registry.PlatformRegistration

data class CompiledSnapshotRequest(val alias: String, val request: SkillSnapshotRequest, val local: Boolean,
    val resource: PlatformRegistration<SkillResourceProvider>? = null, val stat: PlatformRegistration<SkillStatProvider>? = null)
