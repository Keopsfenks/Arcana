package com.keopsfenks.arcana.skills.platform.compile

import com.keopsfenks.arcana.core.math.ExpressionEnvironment
import com.keopsfenks.arcana.skills.platform.DefaultSkillPlatform

/** Retains the source-preflight entry point while the full native compiler owns compilation. */
class SourceSkillCompiler(platform: DefaultSkillPlatform, environment: ExpressionEnvironment, namespace: String,
    environmentFactory: ((Array<String>) -> ExpressionEnvironment)? = null) : PlatformSkillCompiler(platform, environment, namespace, environmentFactory)
