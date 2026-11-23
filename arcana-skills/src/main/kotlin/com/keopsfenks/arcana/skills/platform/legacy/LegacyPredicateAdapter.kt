package com.keopsfenks.arcana.skills.platform.legacy

import com.keopsfenks.arcana.core.skills.CompiledSkillCondition
import com.keopsfenks.arcana.core.skills.platform.*

/** Explicit Player capability boundary; callers retain the legacy actor scheduling requirement. */
class LegacyPredicateAdapter(private val predicate: CompiledSkillCondition) : SkillPredicate {
    override fun test(invocation: SkillInvocation) = predicate.test(LegacyInvocationAdapter.toLegacy(invocation))
}
