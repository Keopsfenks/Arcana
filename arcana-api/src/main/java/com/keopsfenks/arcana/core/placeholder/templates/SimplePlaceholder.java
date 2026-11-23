package com.keopsfenks.arcana.core.placeholder.templates;

import com.keopsfenks.arcana.core.ArcanaPlugin;
import com.keopsfenks.arcana.core.placeholder.RegistrablePlaceholder;
import com.keopsfenks.arcana.core.placeholder.context.PlaceholderContext;
import com.keopsfenks.arcana.util.PatternUtils;
import java.util.Objects;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * A template class for simple registrable placeholders, matched against a literal identifier.
 */
public abstract class SimplePlaceholder implements RegistrablePlaceholder {
    /**
     * The plugin that owns the placeholder.
     */
    private final ArcanaPlugin plugin;

    /**
     * The identifier of the placeholder.
     */
    private final String identifier;

    /**
     * The placeholder pattern.
     */
    private final Pattern pattern;

    /**
     * Create a new simple placeholder.
     *
     * @param plugin     The plugin.
     * @param identifier The identifier.
     */
    protected SimplePlaceholder(@NotNull final ArcanaPlugin plugin,
                                @NotNull final String identifier) {
        this.plugin = plugin;
        this.identifier = identifier;
        this.pattern = PatternUtils.compileLiteral(identifier);
    }

    @Override
    public String tryTranslateQuickly(@NotNull final String text,
                                      @NotNull final PlaceholderContext context) {
        return text.replace(
                "%" + this.identifier + "%",
                Objects.requireNonNullElse(this.getValue(this.identifier, context), "")
        );
    }

    @Override
    public @NotNull Pattern getPattern() {
        return pattern;
    }

    @NotNull
    @Override
    public ArcanaPlugin getPlugin() {
        return plugin;
    }

    @Override
    public String toString() {
        return "SimplePlaceholder{" +
                "plugin='" + plugin + '\'' +
                "identifier='" + identifier + '\'' +
                '}';
    }

    @Override
    public boolean equals(@NotNull final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof SimplePlaceholder that)) {
            return false;
        }

        return Objects.equals(pattern, that.getPattern())
                && Objects.equals(plugin, that.getPlugin());
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, plugin);
    }
}
