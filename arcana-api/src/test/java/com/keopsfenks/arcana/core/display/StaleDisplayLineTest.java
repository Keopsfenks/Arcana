package com.keopsfenks.arcana.core.display;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StaleDisplayLineTest {
    /**
     * A line added by a display module.
     */
    private static LoreNode arcanaLine(final String text) {
        return LoreNode.display(Component.text(text, NamedTextColor.GRAY));
    }

    /**
     * A display line after something has round-tripped the lore through legacy strings, which
     * revert doesn't recognise, so it is left on the item as a foreign line.
     */
    private static LoreNode flattenedLine(final String text) {
        return LoreNode.foreign(
                Component.empty()
                        .append(Component.text(Display.PREFIX))
                        .append(Component.text(text, NamedTextColor.GRAY))
        );
    }

    private static List<LoreNode> withoutStaleLines(final LoreNode... nodes) {
        DisplayLoreBuilder lore = new DisplayLoreBuilder(new ArrayList<>(List.of(nodes)));
        lore.removeStaleLines();
        return lore.getNodes();
    }

    @Test
    public void testNothingIsRemovedWhenNoLineIsWrittenTwice() {
        LoreNode foreign = LoreNode.foreign(Component.text("Foreign lore"));
        LoreNode arcana = arcanaLine("Display line");

        Assertions.assertEquals(List.of(foreign, arcana), withoutStaleLines(foreign, arcana));
    }

    @Test
    public void testStaleLineLeftBehindByRevertIsRemoved() {
        LoreNode arcana = arcanaLine("Display line");

        Assertions.assertEquals(List.of(arcana), withoutStaleLines(flattenedLine("Display line"), arcana));
    }

    @Test
    public void testEveryStaleLineOfAMultiLineItemIsRemoved() {
        LoreNode separator = arcanaLine("----------------");
        LoreNode talent = arcanaLine("Talent");

        Assertions.assertEquals(
                List.of(separator, talent),
                withoutStaleLines(flattenedLine("----------------"), flattenedLine("Talent"), separator, talent)
        );
    }

    @Test
    public void testForeignLoreIsKeptWhenAStaleLineIsRemoved() {
        LoreNode foreign = LoreNode.foreign(Component.text("Enchantment description"));
        LoreNode arcana = arcanaLine("Display line");

        Assertions.assertEquals(
                List.of(foreign, arcana),
                withoutStaleLines(foreign, flattenedLine("Display line"), arcana)
        );
    }

    @Test
    public void testOnlyAsManyCopiesAreRemovedAsWereWrittenAgain() {
        LoreNode kept = flattenedLine("Line");
        LoreNode arcana = arcanaLine("Line");

        Assertions.assertEquals(
                List.of(kept, arcana),
                withoutStaleLines(flattenedLine("Line"), kept, arcana)
        );
    }

    @Test
    public void testEmptyLoreIsLeftAlone() {
        LoreNode arcana = arcanaLine("Line");

        Assertions.assertEquals(List.of(), withoutStaleLines());
        Assertions.assertEquals(List.of(arcana), withoutStaleLines(arcana));
    }

    @Test
    public void testAdvancedEnchantmentsLineIsKept() {
        LoreNode advancedEnchantments = flattenedLine("Chance to harvest in 3x3 area.");
        LoreNode arcana = arcanaLine("Display line");

        Assertions.assertEquals(List.of(advancedEnchantments, arcana), withoutStaleLines(advancedEnchantments, arcana));
    }

    @Test
    public void testAdvancedEnchantmentsLineIsNotADisplayLine() {
        Assertions.assertFalse(DisplayLines.isDisplayLine(flattenedLine("Chance to harvest in 3x3 area.").component()));
    }

    @Test
    public void testRepeatedForeignLineWithoutPrefixIsKept() {
        LoreNode foreign = LoreNode.foreign(Component.text("Line", NamedTextColor.GRAY));
        LoreNode arcana = arcanaLine("Line");

        Assertions.assertEquals(List.of(foreign, arcana), withoutStaleLines(foreign, arcana));
    }
}
