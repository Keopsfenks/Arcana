package com.keopsfenks.arcana.skills.components
import com.keopsfenks.arcana.core.skills.*
import com.keopsfenks.arcana.core.skills.platform.*
import com.keopsfenks.arcana.skills.components.interceptors.*
import com.keopsfenks.arcana.skills.platform.events.ScopedEventAccess
import io.mockk.*
import org.bukkit.event.entity.EntityDamageEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class InterceptorTests {
    @Test fun separateDamageAndCancelComponentsAreSynchronousAndValidateFiniteBounds() {
        val f = TargeterFixture(); var damage = 4.0; var cancelled = false
        val event = mockk<EntityDamageEvent>()
        every { event.damage } answers { damage }; every { event.setDamage(any<Double>()) } answers { damage = firstArg() }
        every { event.isCancelled } answers { cancelled }; every { event.isCancelled = any() } answers { cancelled = firstArg() }
        val admission = mockk<SkillAdmission>()
        ScopedEventAccess(event).use { access ->
            assertTrue(AddDamageInterceptor().compile(SkillArguments(mapOf("amount" to 2), "add"), f.compiler).apply(access, f.invocation, admission).isSuccess)
            assertEquals(6.0, damage)
            assertTrue(MultiplyDamageInterceptor().compile(SkillArguments(mapOf("factor" to 3), "multiply"), f.compiler).apply(access, f.invocation, admission).isSuccess)
            assertEquals(18.0, damage)
            assertTrue(CancelEventInterceptor().compile(SkillArguments(emptyMap<String, Any>(), "cancel"), f.compiler).apply(access, f.invocation, admission).isSuccess)
            assertTrue(cancelled)
            assertThrows(IllegalArgumentException::class.java) { access.damage(Double.NaN) }
        }
        assertThrows(IllegalArgumentException::class.java) { AddDamageInterceptor().compile(SkillArguments(mapOf("amount" to -1), "add"), f.compiler) }
        assertThrows(IllegalArgumentException::class.java) { MultiplyDamageInterceptor().compile(SkillArguments(mapOf("factor" to -1), "multiply"), f.compiler) }
    }
    @Test fun cancelInterceptorReportsUnsupportedForANoncancellableEvent() {
        val f = TargeterFixture()
        ScopedEventAccess(mockk<org.bukkit.event.Event>()).use { access ->
            val interceptor = CancelEventInterceptor().compile(SkillArguments(emptyMap<String, Any>(), "cancel"), f.compiler)
            assertEquals(SkillResult.Status.UNSUPPORTED_CONTEXT, interceptor.apply(access, f.invocation, mockk()).status())
        }
    }

}
