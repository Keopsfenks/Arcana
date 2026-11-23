package com.keopsfenks.arcana.internal.spigot

import com.keopsfenks.arcana.core.Arcana
import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.skills.SkillEngine
import com.keopsfenks.arcana.core.command.impl.PluginCommand
import com.keopsfenks.arcana.internal.spigot.command.ArcanaCommand
import com.keopsfenks.arcana.skills.DefaultSkillEngine
import com.keopsfenks.arcana.skills.BuiltinComponents
import com.keopsfenks.arcana.skills.SkillsRuntime
import com.keopsfenks.arcana.skills.compile.SkillCompilerImpl
import com.keopsfenks.arcana.core.FoliaSupport
import com.keopsfenks.arcana.core.actionbar.ActionBarProvider
import com.keopsfenks.arcana.core.actionbar.PersistentActionBar
import com.keopsfenks.arcana.core.entities.ai.EntityController
import com.keopsfenks.arcana.core.bstats.ArcanaMetricsChart
import com.keopsfenks.arcana.core.enchant.CustomEnchantment
import com.keopsfenks.arcana.core.enchant.VanillaEnchantmentOverrides
import com.keopsfenks.arcana.core.integrations.anticheat.AnticheatManager
import com.keopsfenks.arcana.core.integrations.antigrief.AntigriefManager
import com.keopsfenks.arcana.core.integrations.customitems.CustomItemsManager
import com.keopsfenks.arcana.core.integrations.hologram.Hologram
import com.keopsfenks.arcana.core.integrations.hologram.HologramOptions
import com.keopsfenks.arcana.internal.spigot.hologram.ArcanaHologram
import com.keopsfenks.arcana.internal.spigot.hologram.HologramTracker
import com.keopsfenks.arcana.internal.spigot.proxies.HologramProxy
import com.keopsfenks.arcana.core.PluginLike
import com.keopsfenks.arcana.core.PluginProps
import com.keopsfenks.arcana.core.Prerequisite
import com.keopsfenks.arcana.core.blocks.Blocks
import com.keopsfenks.arcana.core.command.CommandBase
import com.keopsfenks.arcana.core.command.PluginCommandBase
import com.keopsfenks.arcana.core.config.ConfigType
import com.keopsfenks.arcana.core.config.interfaces.Config
import com.keopsfenks.arcana.core.config.interfaces.LoadableConfig
import com.keopsfenks.arcana.core.data.PlayerProfileResolver
import com.keopsfenks.arcana.core.data.keys.PersistentDataKey
import com.keopsfenks.arcana.core.datapack.DatapackContributor
import com.keopsfenks.arcana.core.display.DisplayRecorder
import com.keopsfenks.arcana.core.gui.menu.Menu
import com.keopsfenks.arcana.core.gui.menu.MenuType
import com.keopsfenks.arcana.core.gui.slot.functional.SlotProvider
import com.keopsfenks.arcana.core.gui.view.LocationViewBuilder
import com.keopsfenks.arcana.core.gui.view.MerchantViewBuilder
import com.keopsfenks.arcana.core.gui.view.ViewBuilder
import com.keopsfenks.arcana.core.items.Items
import com.keopsfenks.arcana.core.leaderboard.Leaderboard
import com.keopsfenks.arcana.core.leaderboard.LeaderboardValueProvider
import com.keopsfenks.arcana.core.leaderboard.PlayerbaseTally
import com.keopsfenks.arcana.core.leaderboard.TallyProvider
import com.keopsfenks.arcana.core.packet.Packet
import com.keopsfenks.arcana.core.placeholder.context.PlaceholderContext
import com.keopsfenks.arcana.core.scheduling.Scheduler
import com.keopsfenks.arcana.core.version.Version
import com.keopsfenks.arcana.internal.ArcanaPropsParser
import com.keopsfenks.arcana.internal.command.ArcanaPluginCommand
import com.keopsfenks.arcana.internal.command.ArcanaSubcommand
import com.keopsfenks.arcana.internal.config.ArcanaConfigSection
import com.keopsfenks.arcana.internal.config.ArcanaLoadableConfig
import com.keopsfenks.arcana.internal.config.ArcanaUpdatableConfig
import com.keopsfenks.arcana.internal.config.handler.SimpleConfigHandler
import com.keopsfenks.arcana.internal.config.lang.JarLangSource
import com.keopsfenks.arcana.internal.config.lang.LangFiles
import com.keopsfenks.arcana.internal.config.toMap
import com.keopsfenks.arcana.internal.drops.ArcanaDropQueue
import com.keopsfenks.arcana.internal.drops.ArcanaFastCollatedDropQueue
import com.keopsfenks.arcana.internal.events.ArcanaEventManager
import com.keopsfenks.arcana.internal.extensions.ArcanaExtensionLoader
import com.keopsfenks.arcana.internal.factory.ArcanaMetadataValueFactory
import com.keopsfenks.arcana.internal.factory.ArcanaNamespacedKeyFactory
import com.keopsfenks.arcana.internal.factory.ArcanaRunnableFactory
import com.keopsfenks.arcana.internal.fast.SafeInternalNamespacedKeyFactory
import com.keopsfenks.arcana.internal.gui.MergedStateMenu
import com.keopsfenks.arcana.internal.gui.menu.ArcanaMenuBuilder
import com.keopsfenks.arcana.internal.gui.menu.renderedInventory
import com.keopsfenks.arcana.internal.gui.slot.ArcanaSlotBuilder
import com.keopsfenks.arcana.internal.gui.view.ArcanaLocationViewBuilder
import com.keopsfenks.arcana.internal.gui.view.ArcanaMerchantViewBuilder
import com.keopsfenks.arcana.internal.gui.view.ArcanaViewBuilder
import com.keopsfenks.arcana.internal.integrations.PAPIExpansion
import com.keopsfenks.arcana.internal.logging.ArcanaLogger
import com.keopsfenks.arcana.internal.logging.NOOPLogger
import com.keopsfenks.arcana.internal.placeholder.PlaceholderParser
import com.keopsfenks.arcana.internal.proxy.ArcanaProxyFactory
import com.keopsfenks.arcana.internal.scheduling.ArcanaSchedulerBukkit
import com.keopsfenks.arcana.internal.scheduling.ArcanaSchedulerFolia
import com.keopsfenks.arcana.internal.spigot.data.DataYml
import com.keopsfenks.arcana.internal.spigot.data.KeyRegistry
import com.keopsfenks.arcana.internal.spigot.data.profiles.ProfileHandler
import com.keopsfenks.arcana.internal.spigot.data.profiles.impl.serverProfileUUID
import com.keopsfenks.arcana.internal.spigot.data.profiles.isSavedLocally
import com.keopsfenks.arcana.internal.spigot.integrations.bstats.MetricHandler
import com.keopsfenks.arcana.internal.spigot.leaderboard.KeyLeaderboardValueProvider
import com.keopsfenks.arcana.internal.spigot.leaderboard.LeaderboardService
import com.keopsfenks.arcana.internal.spigot.math.ExpressionEvaluator
import com.keopsfenks.arcana.internal.spigot.math.api.ArcanaExpressionEnvironmentBuilder
import com.keopsfenks.arcana.internal.spigot.proxies.BukkitCommandsProxy
import com.keopsfenks.arcana.internal.spigot.proxies.CommonsInitializerProxy
import com.keopsfenks.arcana.internal.spigot.proxies.DisplayNameProxy
import com.keopsfenks.arcana.internal.spigot.proxies.DisplayRecordsProxy
import com.keopsfenks.arcana.internal.spigot.proxies.DummyEntityFactoryProxy
import com.keopsfenks.arcana.internal.spigot.proxies.EnchantmentRegistryProxy
import com.keopsfenks.arcana.internal.spigot.proxies.EntityControllerFactoryProxy
import com.keopsfenks.arcana.internal.spigot.proxies.ExtendedPersistentDataContainerFactoryProxy
import com.keopsfenks.arcana.internal.spigot.proxies.FastItemStackFactoryProxy
import com.keopsfenks.arcana.internal.spigot.proxies.MiniMessageTranslatorProxy
import com.keopsfenks.arcana.internal.spigot.proxies.PacketHandlerProxy
import com.keopsfenks.arcana.internal.spigot.proxies.PlayerHandlerProxy
import com.keopsfenks.arcana.internal.spigot.proxies.ItemComponentsProxy
import com.keopsfenks.arcana.internal.spigot.proxies.ItemUseProxy
import com.keopsfenks.arcana.internal.spigot.proxies.SNBTConverterProxy
import com.keopsfenks.arcana.internal.spigot.proxies.SkullProxy
import com.keopsfenks.arcana.internal.spigot.proxies.TPSProxy
import com.keopsfenks.arcana.internal.spigot.proxies.WaypointHandlerProxy
import java.net.URLClassLoader
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.inventory.view.MerchantView
import org.bukkit.inventory.view.builder.InventoryViewBuilder
import org.bukkit.inventory.view.builder.LocationInventoryViewBuilder
import org.bukkit.inventory.MenuType as BukkitMenuType
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.util.Vector

private val loadedArcanaPlugins = ConcurrentHashMap<String, ArcanaPlugin>()
private val DEFAULT_PROFILE_RESOLVER = PlayerProfileResolver { it.uniqueId }

@Suppress("UNUSED")
class ArcanaImpl : ArcanaSpigotPlugin(), Arcana {
    private val skillsRuntimeDelegate = lazy {
        val directory = dataFolder.toPath().resolve("skills")
        val environment = createExpressionEnvironmentBuilder().variables(*SkillCompilerImpl.VARIABLES).build()
        SkillsRuntime(this, DefaultSkillEngine(this, directory, environment, expressionEnvironmentFactory = { names ->
            createExpressionEnvironmentBuilder().variables(*names).build()
        }), directory) { engine, owner ->
            BuiltinComponents.install(engine, owner)
            BuiltinComponents.installTriggers(engine, owner)
        }
    }
    private val skillsRuntime by skillsRuntimeDelegate

    override fun getSkillEngine(): SkillEngine {
        check(skillsRuntime.isStarted) { "Arcana Skills is available after Arcana enables" }
        return skillsRuntime.engine
    }

    override fun loadPluginCommands(): List<PluginCommand> = listOf(ArcanaCommand(this) { getSkillEngine() })

    override fun prepareReload(cancelTasks: Boolean) = skillsRuntime.prepareReload(cancelTasks)
    // Lazy, because constructing the config recreates data.yml from the jar. A server that has
    // finished migrating never touches it, and so never grows the file back.
    override val dataYml: DataYml by lazy { DataYml(this) }

    override val profileHandler = ProfileHandler(this)

    val hologramTracker: HologramTracker by lazy { HologramTracker(this) }

    private val leaderboardService = LeaderboardService(this)

    init {
        getProxy(CommonsInitializerProxy::class.java).init(this)
    }

    private val keyFactory = SafeInternalNamespacedKeyFactory()

    private val placeholderParser = PlaceholderParser(
        progressBarCharacter = this.configYml.getString("progress-bar.character").firstOrNull() ?: '|',
        progressBarBars = this.configYml.getInt("progress-bar.bars"),
        progressBarCompleteFormat = this.configYml.getString("progress-bar.complete-format"),
        progressBarInProgressFormat = this.configYml.getString("progress-bar.in-progress-format"),
        progressBarIncompleteFormat = this.configYml.getString("progress-bar.incomplete-format")
    )

    private val expressionEvaluator = ExpressionEvaluator(
        placeholderParser,
        this.configYml.getInt("math-cache-ttl").toLong()
    )

    /**
     * The return type must stay the [Scheduler] interface, and must stay explicit.
     *
     * [ArcanaSchedulerFolia] names Folia types, which are absent on Spigot. The JVM verifier
     * does not check assignability to an interface, so neither branch forces that class to
     * be resolved here; it is loaded only when its constructor actually runs, which happens
     * only on Folia. Narrowing this to a concrete type would make Spigot resolve it at
     * verification and every Spigot server would die at startup with NoClassDefFoundError.
     */
    override fun createScheduler(plugin: ArcanaPlugin): Scheduler =
        if (Prerequisite.HAS_FOLIA.isMet) {
            ArcanaSchedulerFolia(plugin)
        } else {
            ArcanaSchedulerBukkit(plugin)
        }

    override fun createEventManager(plugin: ArcanaPlugin) =
        ArcanaEventManager(plugin)

    override fun createNamespacedKeyFactory(plugin: ArcanaPlugin) =
        ArcanaNamespacedKeyFactory(plugin)

    override fun createMetadataValueFactory(plugin: ArcanaPlugin) =
        ArcanaMetadataValueFactory(plugin)

    override fun createRunnableFactory(plugin: ArcanaPlugin) =
        ArcanaRunnableFactory(plugin)

    override fun createExtensionLoader(plugin: ArcanaPlugin) =
        ArcanaExtensionLoader(plugin)

    override fun createConfigHandler(plugin: ArcanaPlugin) =
        SimpleConfigHandler()

    override fun createLogger(plugin: ArcanaPlugin) =
        ArcanaLogger(plugin)

    override fun getNOOPLogger() =
        NOOPLogger

    override fun createPAPIIntegration(plugin: ArcanaPlugin) {
        PAPIExpansion(plugin)
    }

    override fun getArcanaPlugin(): ArcanaPlugin =
        this

    override fun createConfig(contents: String, type: ConfigType) =
        ArcanaConfigSection(type, type.toMap(contents))

    override fun createConfig(values: Map<String, Any>, type: ConfigType) =
        ArcanaConfigSection(type, values)

    override fun createLoadableConfig(
        configName: String,
        plugin: PluginLike,
        subDirectoryPath: String,
        source: Class<*>,
        type: ConfigType,
        requiresChangesToSave: Boolean
    ) = ArcanaLoadableConfig(
        type,
        configName,
        plugin,
        subDirectoryPath,
        source,
        requiresChangesToSave
    )

    /**
     * arcana's own lang.yml is what tells an existing server apart from a new one.
     */
    override fun createLangConfig(plugin: ArcanaPlugin): LoadableConfig {
        val migrated = LangFiles.migrate(plugin.dataFolder, plugin.logger)

        if (plugin === this && migrated) {
            this.configYml["language"] = LangFiles.EXISTING
            this.configYml.save()
        }

        LangFiles.sync(plugin, JarLangSource.open(plugin.file, plugin.logger))

        return LangFiles.select(plugin, this.configYml.getStringOrNull("language"), migrated)
    }

    override fun createUpdatableConfig(
        configName: String,
        plugin: PluginLike,
        subDirectoryPath: String,
        source: Class<*>,
        removeUnused: Boolean,
        type: ConfigType,
        requiresChangesToSave: Boolean,
        vararg updateBlacklist: String
    ) = ArcanaUpdatableConfig(
        type,
        configName,
        plugin,
        subDirectoryPath,
        source,
        removeUnused,
        requiresChangesToSave,
        *updateBlacklist
    )

    override fun wrapConfigurationSection(bukkit: ConfigurationSection): Config {
        val config = createConfig(emptyMap(), ConfigType.YAML)
        for (key in bukkit.getKeys(true)) {
            config.set(key, bukkit.get(key))
        }

        return config
    }

    override fun createPluginCommand(
        parentDelegate: PluginCommandBase,
        plugin: ArcanaPlugin,
        name: String,
        permission: String,
        playersOnly: Boolean
    ) = ArcanaPluginCommand(
        parentDelegate,
        plugin,
        name,
        permission,
        playersOnly
    )

    override fun createSubcommand(
        parentDelegate: CommandBase,
        plugin: ArcanaPlugin,
        name: String,
        permission: String,
        playersOnly: Boolean
    ) = ArcanaSubcommand(
        parentDelegate,
        plugin,
        name,
        permission,
        playersOnly
    )

    override fun createDropQueue(player: Player) =
        if (this.configYml.getBool("use-fast-collated-drops"))
            ArcanaFastCollatedDropQueue(player) else ArcanaDropQueue(player)

    override fun getRegisteredPersistentDataKeys() =
        KeyRegistry.getRegisteredKeys()

    override fun registerPersistentKey(key: PersistentDataKey<*>) =
        KeyRegistry.registerKey(key)

    override fun createMenuBuilder(rows: Int, type: MenuType) =
        ArcanaMenuBuilder(rows, type.columns)

    override fun createSlotBuilder(provider: SlotProvider) =
        ArcanaSlotBuilder(provider)

    override fun blendMenuState(base: Menu, additional: Menu) =
        MergedStateMenu(base, additional)

    override fun <V : InventoryView> createViewBuilder(
        type: BukkitMenuType.Typed<V, out InventoryViewBuilder<V>>
    ): ViewBuilder<V> = ArcanaViewBuilder<V, InventoryViewBuilder<V>>(type.builder())

    override fun <V : InventoryView> createLocationViewBuilder(
        type: BukkitMenuType.Typed<V, LocationInventoryViewBuilder<V>>
    ): LocationViewBuilder<V> = ArcanaLocationViewBuilder(type.builder())

    override fun createMerchantViewBuilder(): MerchantViewBuilder<MerchantView> =
        ArcanaMerchantViewBuilder(BukkitMenuType.MERCHANT.builder())

    override fun clean(plugin: ArcanaPlugin) {
        // Prevent self-cleaning
        if (plugin == this) {
            return
        }

        if (plugin.proxyPackage.isNotEmpty()) {
            val factory = plugin.proxyFactory as ArcanaProxyFactory
            factory.clean()
        }

        loadedArcanaPlugins.remove(plugin.name.lowercase())

        persistentActionBars.unregisterAll(plugin)

        for (customItem in Items.getCustomItems()) {
            if (customItem.key.namespace.equals(plugin.name.lowercase(), ignoreCase = true)) {
                Items.removeCustomItem(customItem.key)
            }
        }

        for (customBlock in Blocks.getCustomBlocks()) {
            if (customBlock.key.namespace.equals(plugin.name.lowercase(), ignoreCase = true)) {
                Blocks.removeCustomBlock(customBlock.key)
            }
        }

        val classLoader = plugin::class.java.classLoader

        if (classLoader is URLClassLoader) {
            classLoader.close()
        }

        System.gc()
    }

    override fun createProxyFactory(plugin: ArcanaPlugin) =
        ArcanaProxyFactory(plugin)

    override fun addNewPlugin(plugin: ArcanaPlugin) {
        loadedArcanaPlugins[plugin.name.lowercase()] = plugin
        loadedArcanaPlugins[plugin.id] = plugin
    }

    override fun registerOutdatedPlugin(pluginName: String, requiredVersion: Version) =
        OutdatedPlugins.register(pluginName, requiredVersion)

    override fun getLoadedPlugins(): List<String> =
        loadedArcanaPlugins.keys.toList()

    override fun getPluginByName(name: String): ArcanaPlugin? =
        loadedArcanaPlugins[name.lowercase()]

    override fun createFastItemStack(itemStack: ItemStack) =
        getProxy(FastItemStackFactoryProxy::class.java).create(itemStack)

    override fun registerBStats(plugin: ArcanaPlugin) =
        MetricHandler.createMetrics(plugin)

    override fun getAdventure() =
        bukkitAudiences

    override fun getServerProfile() =
        profileHandler.getServerProfile()

    override fun loadPlayerProfile(uuid: UUID) =
        profileHandler.getPlayerProfile(uuid)

    override fun getSavedProfileUUIDs(): Set<UUID> =
        profileHandler.defaultHandler.getSavedUUIDs() - serverProfileUUID

    override fun runWithTransientProfile(uuid: UUID, action: Runnable) {
        profileHandler.runWithTransientProfile(uuid, action)
    }

    override fun <T> readAllProfileValues(
        uuids: Set<UUID>,
        key: PersistentDataKey<T>
    ): Map<UUID, T> {
        val handler = if (key.isSavedLocally) {
            profileHandler.localHandler
        } else {
            profileHandler.defaultHandler
        }

        return handler.readAll(uuids, key)
    }

    override fun readAllProfileValuesForKeys(
        uuids: Set<UUID>,
        keys: Collection<PersistentDataKey<*>>
    ): Map<PersistentDataKey<*>, Map<UUID, Any>> {
        // Split by storage location: a locally-saved key lives in the YAML handler rather than the
        // configured one, so batching them together would read the wrong store for half of them.
        val (local, default) = keys.partition { it.isSavedLocally }

        val values = HashMap<PersistentDataKey<*>, Map<UUID, Any>>(keys.size)

        if (local.isNotEmpty()) {
            values.putAll(profileHandler.localHandler.readAllKeys(uuids, local))
        }

        if (default.isNotEmpty()) {
            values.putAll(profileHandler.defaultHandler.readAllKeys(uuids, default))
        }

        return values
    }

    override fun registerLeaderboard(
        plugin: ArcanaPlugin,
        id: String,
        provider: LeaderboardValueProvider
    ): Leaderboard = leaderboardService.register(plugin, id, provider)

    override fun registerKeyLeaderboard(
        plugin: ArcanaPlugin,
        id: String,
        key: PersistentDataKey<*>
    ): Leaderboard = leaderboardService.register(plugin, id, KeyLeaderboardValueProvider(key))

    override fun getLeaderboard(id: String): Leaderboard? =
        leaderboardService.get(id)

    override fun getLeaderboards(): Collection<Leaderboard> =
        leaderboardService.values()

    override fun unregisterLeaderboards(plugin: ArcanaPlugin) =
        leaderboardService.unregisterAll(plugin)

    override fun registerTally(
        plugin: ArcanaPlugin,
        id: String,
        provider: TallyProvider
    ): PlayerbaseTally = leaderboardService.registerTally(plugin, id, provider)

    override fun getTally(id: String): PlayerbaseTally? =
        leaderboardService.getTally(id)

    override fun getTallies(): Collection<PlayerbaseTally> =
        leaderboardService.tallies()

    override fun registerPersistentActionBar(
        plugin: ArcanaPlugin,
        id: String,
        priority: Int,
        provider: ActionBarProvider
    ): PersistentActionBar = persistentActionBars.register(plugin, id, priority, provider)

    override fun refreshPersistentActionBar(player: Player) =
        persistentActionBars.refresh(player)

    // Read from whichever thread touches player data, so publication has to be guaranteed.
    @Volatile
    private var playerProfileResolver = DEFAULT_PROFILE_RESOLVER

    override fun setPlayerProfileResolver(resolver: PlayerProfileResolver?) {
        playerProfileResolver = if (resolver == null) DEFAULT_PROFILE_RESOLVER else {
            // Wrapped rather than stored bare so every profile a player resolves to is recorded and
            // can be unloaded when they leave. Only wrapped when a resolver is actually set, so the
            // default path stays a plain UUID read.
            PlayerProfileResolver { player ->
                val profile = resolver.resolve(player)
                profileHandler.trackResolvedProfile(player.uniqueId, profile)
                profile
            }
        }
    }

    override fun getPlayerProfileResolver() = playerProfileResolver

    override fun createDummyEntity(location: Location): Entity {
        warnIfNotOwned(location, "Creating a dummy entity")

        return getProxy(DummyEntityFactoryProxy::class.java).createDummyEntity(location)
    }

    override fun createHologram(location: Location, options: HologramOptions): Hologram {
        warnIfNotOwned(location, "Creating a hologram")

        val handle = getProxy(HologramProxy::class.java).createHandle(location, options)
        return ArcanaHologram(handle, location, options, hologramTracker)
    }

    override fun handleEnable() {
        super.handleEnable()
        skillsRuntime.start()
        hologramTracker.start()

        // Fed from the writer rather than polled: every value change on this server lands in the
        // leaderboard caches on the tick it is committed, so the database sweep is only needed to
        // pick up writes made by something other than this server.
        profileHandler.profileWriter.onWrite = { uuid, key, value ->
            leaderboardService.onValueWritten(uuid, key, value)
        }

        // Nothing is ranked while profiles are still being carried out of data.yml, and the
        // caches are loaded once that finishes rather than waiting for the next reconcile - a
        // server with reconciliation turned off would otherwise rank nobody until it restarted.
        leaderboardService.isPaused = { profileHandler.liveMigration != null }
        profileHandler.onLiveMigrationComplete = { leaderboardService.start() }

        // profileHandler is constructed with ArcanaImpl itself, and Arcana.Instance is set in the
        // ArcanaPlugin constructor, so savedProfileUUIDs is already answerable here. The first
        // sweep is a second away in any case, well after afterLoad() two ticks in.
        leaderboardService.start()
    }

    override fun handleReload() {
        super.handleReload()
        try { skillsRuntime.afterReload() }
        finally {
            // Independent services still restart when a staged skills dependency disappears.
            leaderboardService.start()
        }
    }

    override fun handleDisable() {
        if (skillsRuntimeDelegate.isInitialized()) skillsRuntime.stop()
        leaderboardService.shutdown()
        super.handleDisable()
        hologramTracker.shutdown()
    }

    override fun createNamespacedKey(namespace: String, key: String) =
        NamespacedKey(namespace, key)

    override fun getProps(existing: PluginProps?, plugin: Class<out ArcanaPlugin>) =
        existing ?: ArcanaPropsParser.parseForPlugin(plugin)

    override fun <T : Mob> createEntityController(mob: T): EntityController<T> {
        warnIfNotOwned(mob, "Creating an entity controller")

        return getProxy(EntityControllerFactoryProxy::class.java).createEntityController(mob)
    }

    override fun formatMiniMessage(message: String) =
        getProxy(MiniMessageTranslatorProxy::class.java).format(message)

    override fun getDisplayRecorder(): DisplayRecorder =
        getProxy(DisplayRecordsProxy::class.java)

    override fun requestDisplayRefresh() =
        displayRefresher.request()

    override fun adaptPdc(container: PersistentDataContainer) =
        getProxy(ExtendedPersistentDataContainerFactoryProxy::class.java).adapt(container)

    override fun newPdc() =
        getProxy(ExtendedPersistentDataContainerFactoryProxy::class.java).newPdc()

    override fun toSNBT(itemStack: ItemStack) =
        getProxy(SNBTConverterProxy::class.java).toSNBT(itemStack)

    override fun fromSNBT(snbt: String) =
        getProxy(SNBTConverterProxy::class.java).fromSNBT(snbt)

    override fun testableItemFromSNBT(snbt: String) =
        getProxy(SNBTConverterProxy::class.java).makeSNBTTestable(snbt)

    override fun withComponents(item: ItemStack, components: Map<String, Any>) =
        getProxy(ItemComponentsProxy::class.java).withComponents(item, components)

    override fun getComponents(item: ItemStack) =
        getProxy(ItemComponentsProxy::class.java).getComponents(item)

    override fun getComponent(item: ItemStack, key: String) =
        getProxy(ItemComponentsProxy::class.java).getComponent(item, key)

    override fun removeComponents(item: ItemStack, keys: Collection<String>) =
        getProxy(ItemComponentsProxy::class.java).removeComponents(item, keys)

    override fun getSkullTexture(meta: SkullMeta): String? =
        getProxy(SkullProxy::class.java).getSkullTexture(meta)

    override fun setSkullTexture(meta: SkullMeta, base64: String) =
        getProxy(SkullProxy::class.java).setSkullTexture(meta, base64)

    override fun getTPS() =
        getProxy(TPSProxy::class.java).getTPS()

    override fun isOwnedByCurrentRegion(location: Location): Boolean =
        if (Prerequisite.HAS_FOLIA.isMet) Bukkit.isOwnedByCurrentRegion(location) else true

    override fun isOwnedByCurrentRegion(entity: Entity): Boolean =
        if (Prerequisite.HAS_FOLIA.isMet) Bukkit.isOwnedByCurrentRegion(entity) else true

    /**
     * Warn once when a region-bound call is made from a thread that does not own the
     * region. Does not stop the call: Folia will refuse it and say so far more precisely
     * than this can, and off Folia there is nothing to warn about.
     */
    private fun warnIfNotOwned(location: Location, what: String) {
        if (isOwnedByCurrentRegion(location)) {
            return
        }

        FoliaSupport.isUnsupported("$what from outside its region")
    }

    /**
     * Warn once when a region-bound call is made from a thread that does not own the
     * region containing an entity. See [warnIfNotOwned] for the location overload.
     */
    private fun warnIfNotOwned(entity: Entity, what: String) {
        if (isOwnedByCurrentRegion(entity)) {
            return
        }

        FoliaSupport.isUnsupported("$what from outside its region")
    }

    /**
     * Warn once when a global-region-bound call is made from a thread that is not the
     * global region thread. Same idea as [warnIfNotOwned], for calls that belong on the
     * global region rather than on a location or entity's region. Does not stop the
     * call: see [warnIfNotOwned] for why.
     */
    private fun warnIfNotGlobalRegion(what: String) {
        if (!Prerequisite.HAS_FOLIA.isMet || Bukkit.isGlobalTickThread()) {
            return
        }

        FoliaSupport.isUnsupported("$what from outside the global region")
    }

    /**
     * Run on the region owning an entity, now if this thread already owns it.
     *
     * Off Folia the ownership check is always true, so this is a direct call.
     */
    private inline fun onEntity(entity: Entity, crossinline block: () -> Unit) {
        if (isOwnedByCurrentRegion(entity)) {
            block()
        } else {
            this.scheduler.on(entity).run { block() }
        }
    }

    /**
     * Run on the global region, now if this thread is already the global region thread.
     */
    private inline fun onGlobalRegion(crossinline block: () -> Unit) {
        if (!Prerequisite.HAS_FOLIA.isMet || Bukkit.isGlobalTickThread()) {
            block()
        } else {
            this.scheduler.global().run { block() }
        }
    }

    override fun evaluate(expression: String, context: PlaceholderContext) =
        expressionEvaluator.evaluate(expression, context)

    override fun createExpressionEnvironmentBuilder() =
        ArcanaExpressionEnvironmentBuilder()

    override fun getDatapackHandle(plugin: ArcanaPlugin) =
        datapackRegistry.handle(plugin)

    override fun registerDatapackContributor(plugin: ArcanaPlugin, contributor: DatapackContributor) =
        datapackRegistry.register(plugin, contributor)

    override fun isDatapackRestartPending() =
        datapackRegistry.restartPending

    override fun getOpenMenu(player: Player) =
        player.renderedInventory?.menu

    override fun addBukkitRecipeNoResend(recipe: Recipe) = onGlobalRegion {
        this.getProxy(CommonsInitializerProxy::class.java).addBukkitRecipeNoResend(recipe)
    }

    override fun reloadBukkitRecipes() = onGlobalRegion {
        this.getProxy(CommonsInitializerProxy::class.java).reloadBukkitRecipes()
    }

    // Not routed through onGlobalRegion: this returns a Boolean, and onGlobalRegion's
    // block is `() -> Unit`, so it cannot carry a result back from a scheduled task
    // without either blocking the caller or inventing a placeholder return value, neither
    // of which the task brief specifies. Runs inline, exactly as before, on both Paper
    // and Folia; warnIfNotGlobalRegion only warns (it never throws or reschedules) when
    // called off the global region on Folia. See task-19-report.md.
    override fun removeBukkitRecipeNoResend(key: NamespacedKey): Boolean {
        warnIfNotGlobalRegion("Removing a recipe without resending it")
        return this.getProxy(CommonsInitializerProxy::class.java).removeBukkitRecipeNoResend(key)
    }

    private var batchDepth = 0
    private var syncDuringBatch = false

    override fun syncCommands() {
        if (batchDepth > 0) {
            syncDuringBatch = true
            return
        }
        onGlobalRegion {
            this.getProxy(BukkitCommandsProxy::class.java).syncCommands()
        }
    }

    override fun beginCommandBatch() {
        batchDepth++
    }

    override fun endCommandBatch() {
        check(batchDepth > 0) { "endCommandBatch() called without matching beginCommandBatch()" }
        batchDepth--
        if (batchDepth == 0 && syncDuringBatch) {
            syncDuringBatch = false
            onGlobalRegion {
                this.getProxy(BukkitCommandsProxy::class.java).syncCommands()
            }
        }
    }

    override fun unregisterCommand(command: PluginCommandBase) = onGlobalRegion {
        this.getProxy(BukkitCommandsProxy::class.java).unregisterCommand(command)
    }

    override fun sendPacket(player: Player, packet: Packet) =
        this.getProxy(PacketHandlerProxy::class.java).sendPacket(player, packet)

    override fun showWaypoint(viewer: Player, id: UUID, location: Location, color: Int?) =
        this.getProxy(WaypointHandlerProxy::class.java).showWaypoint(viewer, id, location, color)

    override fun hideWaypoint(viewer: Player, id: UUID) =
        this.getProxy(WaypointHandlerProxy::class.java).hideWaypoint(viewer, id)

    override fun translatePlaceholders(text: String, context: PlaceholderContext) =
        placeholderParser.translatePlacholders(text, context)

    override fun translateExpressions(text: String, context: PlaceholderContext) =
        placeholderParser.translateExpressions(text, context)

    override fun getPlaceholderValue(plugin: ArcanaPlugin?, args: String, context: PlaceholderContext) =
        placeholderParser.getPlaceholderResult(plugin, args, context)

    override fun setClientsideDisplayName(entity: LivingEntity, player: Player, name: Component, visible: Boolean) =
        this.getProxy(DisplayNameProxy::class.java).setClientsideDisplayName(entity, player, name, visible)

    override fun giveExpAndApplyMending(player: Player, amount: Int, applyMending: Boolean) {
        onEntity(player) {
            getProxy(PlayerHandlerProxy::class.java).giveExpAndApplyMending(player, amount, applyMending)
        }
    }

    override fun useItemOn(player: Player, hand: EquipmentSlot, block: Block, face: BlockFace, clickedPosition: Vector?) =
        getProxy(ItemUseProxy::class.java).useItemOn(player, hand, block, face, clickedPosition)

    override fun unfreezeEnchantmentRegistry() =
        getProxy(EnchantmentRegistryProxy::class.java).unfreeze()

    override fun freezeEnchantmentRegistry() =
        getProxy(EnchantmentRegistryProxy::class.java).freeze()

    override fun registerCustomEnchantment(enchantment: CustomEnchantment) =
        getProxy(EnchantmentRegistryProxy::class.java).register(enchantment)

    override fun unregisterCustomEnchantment(enchantment: CustomEnchantment) =
        getProxy(EnchantmentRegistryProxy::class.java).unregister(enchantment)

    override fun setVanillaEnchantmentOverrides(overrides: VanillaEnchantmentOverrides?) =
        getProxy(EnchantmentRegistryProxy::class.java).setVanillaOverrides(overrides)

    override fun getCustomEnchantment(enchantment: Enchantment) =
        getProxy(EnchantmentRegistryProxy::class.java).getCustomEnchantment(enchantment)

    override fun getCustomCharts() = listOf(
        ArcanaMetricsChart.SimplePie("data_handler") { profileHandler.defaultHandler.id },
        ArcanaMetricsChart.SingleLine("loaded_arcana_plugins") {
            loadedArcanaPlugins.values.distinct().size
        },
        ArcanaMetricsChart.SingleLine("loaded_extensions") {
            loadedArcanaPlugins.values.distinct()
                .sumOf { it.extensionLoader.getLoadedExtensions().size }
        },
        ArcanaMetricsChart.AdvancedPie("antigrief_integrations") {
            AntigriefManager.getRegisteredIntegrations()
                .associate { it.pluginName to 1 }
                .ifEmpty { null }
        },
        ArcanaMetricsChart.AdvancedPie("custom_item_integrations") {
            CustomItemsManager.getRegisteredIntegrations()
                .associate { it.pluginName to 1 }
                .ifEmpty { null }
        },
        ArcanaMetricsChart.AdvancedPie("anticheat_integrations") {
            AnticheatManager.getRegisteredIntegrations()
                .associate { it.pluginName to 1 }
                .ifEmpty { null }
        }
    )
}
