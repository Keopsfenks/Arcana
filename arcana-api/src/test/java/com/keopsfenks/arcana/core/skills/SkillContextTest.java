package com.keopsfenks.arcana.core.skills;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SkillContextTest {
    private Player player() {
        UUID id = UUID.randomUUID();
        return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> id;
                    case "toString" -> "TestPlayer";
                    default -> throw new AssertionError("Context construction must not read live state: " + method);
                });
    }

    @Test void contextCopiesItsInputAndDoesNotReadPlayerState() {
        var original = new LinkedHashMap<String, Object>();
        original.put("damage", 4.0);
        var context = SkillContext.builder(player()).values(original).build();
        original.put("damage", 99.0);
        assertEquals(4.0, context.getNumber("damage"));
        assertEquals(0.0, context.getNumber("absent"));
        assertThrows(UnsupportedOperationException.class, () -> context.getValues().put("damage", 8));
    }

    @Test void argumentsAreDeeplyImmutableAndErrorsCarryTheFieldPath() {
        var list = new ArrayList<>(List.of("one"));
        var source = new LinkedHashMap<String, Object>();
        source.put("nested", Map.of("names", list));
        var arguments = new SkillArguments(source, "skills/test.yml.effects[0]");
        list.add("two");
        assertEquals(1, arguments.section("nested").list("names").size());
        assertThrows(UnsupportedOperationException.class, () -> arguments.section("nested").list("names").clear());
        var error = assertThrows(IllegalArgumentException.class, () -> arguments.string("missing"));
        assertTrue(error.getMessage().contains("skills/test.yml.effects[0].missing"));
    }

    @Test void componentIdsAndNumbersAreValidated() {
        assertThrows(IllegalArgumentException.class, () -> new SkillEffect("Bad ID") {
            @Override public CompiledSkillEffect compile(SkillArguments args, SkillCompiler compiler) { return null; }
        });
        assertThrows(IllegalArgumentException.class, () -> SkillContext.builder(player()).value("damage", Double.NaN).build());
    }

    @Test void cyclicArgumentTreesFailWithAPathInsteadOfOverflowingTheStack() {
        var cycle = new LinkedHashMap<String, Object>();
        cycle.put("again", cycle);
        var error = assertThrows(IllegalArgumentException.class, () -> new SkillArguments(cycle, "skill.yml"));
        assertTrue(error.getMessage().contains("skill.yml.again"));
    }

    @Test void mutableNumberValuesAreCopiedIntoTheSnapshot() {
        var number = new AtomicInteger(4);
        var context = SkillContext.builder(player()).value("damage", number).build();
        number.set(9);
        assertEquals(4.0, context.getNumber("damage"));
    }
}
