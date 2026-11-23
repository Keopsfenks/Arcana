package com.keopsfenks.arcana.core.skills;

import org.bukkit.plugin.Plugin;

/** Idempotent ownership handle; closing invalidates dependent skills immediately. */
public interface SkillRegistration extends AutoCloseable {
    String getId();
    Plugin getOwner();
    boolean isActive();
    @Override void close();
}
