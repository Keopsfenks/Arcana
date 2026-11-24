package com.keopsfenks.arcana.skills.platform.events
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.platform.DefaultSkillPlatform

class InterceptorCompiler(private val compilation: SkillCompilation, private val platform: DefaultSkillPlatform, private val dependencies: MutableSet<SkillRegistration>) {
    fun compile(args: SkillArguments): List<CompiledSkillInterceptor> {
        val entries = args.list("interceptors", emptyList<Any>())
        require(entries.size <= 64) { "${args.path}.interceptors: local step limit is 64" }
        return java.util.List.copyOf(entries.mapIndexed { index, raw ->
            val node = args.child("interceptors[$index]", raw)
            val forbidden = setOf("delay", "repeat", "interval", "targeter", "mutators", "effects", "on-failure", "conditions", "chance")
            node.values.keys.firstOrNull { it in forbidden }?.let { throw node.error(it, "unavailable in synchronous interception") }
            val name = node.string("type")
            val local = SkillKey.parse(name, compilation.namespace())
            val key = if (':' in name || platform.registry(SkillComponentType.INTERCEPTOR).get(local) != null) local else SkillKey.parse(name, "arcana")
            val handle = platform.registry(SkillComponentType.INTERCEPTOR).get(key) ?: throw node.error("type", "unknown interceptor $key")
            dependencies.add(handle)
            val compiled = handle.component.compile(node, compilation)
            CompiledSkillInterceptor { event, invocation, admission ->
                if (!platform.componentActive(handle)) SkillResult.of(SkillResult.Status.DEPENDENCY_UNAVAILABLE, "Interceptor $key retired")
                else compiled.apply(event, invocation, admission)
            }
        })
    }
}
