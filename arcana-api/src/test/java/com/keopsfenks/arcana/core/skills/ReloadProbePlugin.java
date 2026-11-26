package com.keopsfenks.arcana.core.skills;

import com.keopsfenks.arcana.core.ArcanaPlugin;

/** Exposes the protected hook for constructor-free lifecycle boundary tests. */
public class ReloadProbePlugin extends ArcanaPlugin {
    @Override public java.util.List<com.keopsfenks.arcana.core.command.impl.PluginCommand> loadPluginCommands() { return super.loadPluginCommands(); }
    @Override public boolean prepareReload(boolean cancelTasks) { return super.prepareReload(cancelTasks); }
    @Override public void handleReload() { super.handleReload(); }
}
