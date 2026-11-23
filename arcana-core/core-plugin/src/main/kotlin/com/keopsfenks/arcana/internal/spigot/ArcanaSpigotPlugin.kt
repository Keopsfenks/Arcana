package com.keopsfenks.arcana.internal.spigot

import com.keopsfenks.arcana.core.Arcana
import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.Prerequisite
import com.keopsfenks.arcana.core.blocks.Blocks
import com.keopsfenks.arcana.core.data.ExternalDataStore
import com.keopsfenks.arcana.core.entities.Entities
import com.keopsfenks.arcana.core.integrations.IntegrationLoader
import com.keopsfenks.arcana.core.integrations.afk.AFKManager
import com.keopsfenks.arcana.core.integrations.anticheat.AnticheatManager
import com.keopsfenks.arcana.core.integrations.antigrief.AntigriefManager
import com.keopsfenks.arcana.core.integrations.customblocks.CustomBlocksManager
import com.keopsfenks.arcana.core.integrations.customentities.CustomEntitiesManager
import com.keopsfenks.arcana.core.integrations.customitems.CustomItemsManager
import com.keopsfenks.arcana.core.integrations.economy.EconomyManager
import com.keopsfenks.arcana.core.integrations.discord.DiscordManager
import com.keopsfenks.arcana.internal.discord.DiscordIntegrationImpl
import com.keopsfenks.arcana.core.integrations.mcmmo.McmmoManager
import com.keopsfenks.arcana.core.integrations.model.ModelManager
import com.keopsfenks.arcana.core.integrations.placeholder.PlaceholderManager
import com.keopsfenks.arcana.core.integrations.shop.ShopManager
import com.keopsfenks.arcana.core.items.Items
import com.keopsfenks.arcana.core.packet.PacketListener
import com.keopsfenks.arcana.core.particle.Particles
import com.keopsfenks.arcana.core.price.PriceFactory
import com.keopsfenks.arcana.core.price.Prices
import com.keopsfenks.arcana.core.recipe.Recipes
import com.keopsfenks.arcana.internal.blocks.BlockArgParserAgeable
import com.keopsfenks.arcana.internal.blocks.BlockArgParserAnaloguePowerable
import com.keopsfenks.arcana.internal.blocks.BlockArgParserAttachable
import com.keopsfenks.arcana.internal.blocks.BlockArgParserBamboo
import com.keopsfenks.arcana.internal.blocks.BlockArgParserBisected
import com.keopsfenks.arcana.internal.blocks.BlockArgParserBubbleColumn
import com.keopsfenks.arcana.internal.blocks.BlockArgParserCake
import com.keopsfenks.arcana.internal.blocks.BlockArgParserCampfire
import com.keopsfenks.arcana.internal.blocks.BlockArgParserCaveVinesPlant
import com.keopsfenks.arcana.internal.blocks.BlockArgParserChest
import com.keopsfenks.arcana.internal.blocks.BlockArgParserCommandBlock
import com.keopsfenks.arcana.internal.blocks.BlockArgParserComparator
import com.keopsfenks.arcana.internal.blocks.BlockArgParserCrafter
import com.keopsfenks.arcana.internal.blocks.BlockArgParserDaylightDetector
import com.keopsfenks.arcana.internal.blocks.BlockArgParserDirectional
import com.keopsfenks.arcana.internal.blocks.BlockArgParserDispenser
import com.keopsfenks.arcana.internal.blocks.BlockArgParserDoor
import com.keopsfenks.arcana.internal.blocks.BlockArgParserEndPortalFrame
import com.keopsfenks.arcana.internal.blocks.BlockArgParserFaceAttachable
import com.keopsfenks.arcana.internal.blocks.BlockArgParserFarmland
import com.keopsfenks.arcana.internal.blocks.BlockArgParserGate
import com.keopsfenks.arcana.internal.blocks.BlockArgParserHangable
import com.keopsfenks.arcana.internal.blocks.BlockArgParserHangingMoss
import com.keopsfenks.arcana.internal.blocks.BlockArgParserJigsaw
import com.keopsfenks.arcana.internal.blocks.BlockArgParserLevelled
import com.keopsfenks.arcana.internal.blocks.BlockArgParserLightable
import com.keopsfenks.arcana.internal.blocks.BlockArgParserMossyCarpet
import com.keopsfenks.arcana.internal.blocks.BlockArgParserMultipleFacing
import com.keopsfenks.arcana.internal.blocks.BlockArgParserNoteBlock
import com.keopsfenks.arcana.internal.blocks.BlockArgParserOpenable
import com.keopsfenks.arcana.internal.blocks.BlockArgParserOrientable
import com.keopsfenks.arcana.internal.blocks.BlockArgParserPointedDripstone
import com.keopsfenks.arcana.internal.blocks.BlockArgParserPowerable
import com.keopsfenks.arcana.internal.blocks.BlockArgParserQuantity
import com.keopsfenks.arcana.internal.blocks.BlockArgParserRail
import com.keopsfenks.arcana.internal.blocks.BlockArgParserRedstoneWire
import com.keopsfenks.arcana.internal.blocks.BlockArgParserRepeater
import com.keopsfenks.arcana.internal.blocks.BlockArgParserRespawnAnchor
import com.keopsfenks.arcana.internal.blocks.BlockArgParserRotatable
import com.keopsfenks.arcana.internal.blocks.BlockArgParserScaffolding
import com.keopsfenks.arcana.internal.blocks.BlockArgParserSculkSensor
import com.keopsfenks.arcana.internal.blocks.BlockArgParserSculkShrieker
import com.keopsfenks.arcana.internal.blocks.BlockArgParserSlab
import com.keopsfenks.arcana.internal.blocks.BlockArgParserSnow
import com.keopsfenks.arcana.internal.blocks.BlockArgParserSnowy
import com.keopsfenks.arcana.internal.blocks.BlockArgParserStairs
import com.keopsfenks.arcana.internal.blocks.BlockArgParserStructureBlock
import com.keopsfenks.arcana.internal.blocks.BlockArgParserTNT
import com.keopsfenks.arcana.internal.blocks.BlockArgParserTrial
import com.keopsfenks.arcana.internal.blocks.BlockArgParserTripwire
import com.keopsfenks.arcana.internal.blocks.BlockArgParserWall
import com.keopsfenks.arcana.internal.blocks.BlockArgParserWaterlogged
import com.keopsfenks.arcana.internal.blocks.tags.VanillaBlockTags
import com.keopsfenks.arcana.internal.entities.tags.VanillaEntityTags
import com.keopsfenks.arcana.internal.data.MavenVersionToStringAdapter
import com.keopsfenks.arcana.internal.data.VersionToStringAdapter
import com.keopsfenks.arcana.internal.entities.EntityArgParserAdult
import com.keopsfenks.arcana.internal.entities.EntityArgParserAttackDamage
import com.keopsfenks.arcana.internal.entities.EntityArgParserAttackSpeed
import com.keopsfenks.arcana.internal.entities.EntityArgParserBaby
import com.keopsfenks.arcana.internal.entities.EntityArgParserCharged
import com.keopsfenks.arcana.internal.entities.EntityArgParserEquipment
import com.keopsfenks.arcana.internal.entities.EntityArgParserExplosionRadius
import com.keopsfenks.arcana.internal.entities.EntityArgParserFirework
import com.keopsfenks.arcana.internal.entities.EntityArgParserFlySpeed
import com.keopsfenks.arcana.internal.entities.EntityArgParserFollowRange
import com.keopsfenks.arcana.internal.entities.EntityArgParserHealth
import com.keopsfenks.arcana.internal.entities.EntityArgParserJumpStrength
import com.keopsfenks.arcana.internal.entities.EntityArgParserKnockback
import com.keopsfenks.arcana.internal.entities.EntityArgParserKnockbackResistance
import com.keopsfenks.arcana.internal.entities.EntityArgParserName
import com.keopsfenks.arcana.internal.entities.EntityArgParserNoAI
import com.keopsfenks.arcana.internal.entities.EntityArgParserScale
import com.keopsfenks.arcana.internal.entities.EntityArgParserSilent
import com.keopsfenks.arcana.internal.entities.EntityArgParserSize
import com.keopsfenks.arcana.internal.entities.EntityArgParserTamed
import com.keopsfenks.arcana.internal.entities.EntityArgParserSpawnReinforcements
import com.keopsfenks.arcana.internal.entities.EntityArgParserSpeed
import com.keopsfenks.arcana.internal.items.ArgParserAttribute
import com.keopsfenks.arcana.internal.items.ArgParserColor
import com.keopsfenks.arcana.internal.items.ArgParserCustomModelData
import com.keopsfenks.arcana.internal.items.ArgParserEnchantment
import com.keopsfenks.arcana.internal.items.ArgParserEntity
import com.keopsfenks.arcana.internal.items.ArgParserFireResistant
import com.keopsfenks.arcana.internal.items.ArgParserFireworkBuilder
import com.keopsfenks.arcana.internal.items.ArgParserFireworkPower
import com.keopsfenks.arcana.internal.items.ArgParserFlag
import com.keopsfenks.arcana.internal.items.ArgParserGlider
import com.keopsfenks.arcana.internal.items.ArgParserGlint
import com.keopsfenks.arcana.internal.items.ArgParserHead
import com.keopsfenks.arcana.internal.items.ArgParserItemModel
import com.keopsfenks.arcana.internal.items.ArgParserItemName
import com.keopsfenks.arcana.internal.items.ArgParserMaxDamage
import com.keopsfenks.arcana.internal.items.ArgParserMaxStackSize
import com.keopsfenks.arcana.internal.items.ArgParserName
import com.keopsfenks.arcana.internal.items.ArgParserPotionBuilder
import com.keopsfenks.arcana.internal.items.ArgParserTexture
import com.keopsfenks.arcana.internal.items.ArgParserTooltipStyle
import com.keopsfenks.arcana.internal.items.ArgParserTrim
import com.keopsfenks.arcana.internal.items.ArgParserUnbreakable
import com.keopsfenks.arcana.internal.items.ArgParserUnconsumable
import com.keopsfenks.arcana.internal.items.ArgParserUnenchantable
import com.keopsfenks.arcana.internal.items.ArgParserUngrindable
import com.keopsfenks.arcana.internal.items.ArgParserUseCooldown
import com.keopsfenks.arcana.internal.items.tags.VanillaItemTags
import com.keopsfenks.arcana.internal.lookup.SegmentParserGroup
import com.keopsfenks.arcana.internal.lookup.SegmentParserUseIfPresent
import com.keopsfenks.arcana.internal.particle.ParticleFactoryDustTransition
import com.keopsfenks.arcana.internal.particle.ParticleFactoryEntityEffect
import com.keopsfenks.arcana.internal.particle.ParticleFactoryRGB
import com.keopsfenks.arcana.internal.price.PriceFactoryEconomy
import com.keopsfenks.arcana.internal.price.PriceFactoryXP
import com.keopsfenks.arcana.internal.price.PriceFactoryXPLevels
import com.keopsfenks.arcana.internal.spigot.actionbar.PersistentActionBarService
import com.keopsfenks.arcana.internal.spigot.actionbar.PersistentActionBarTicker
import com.keopsfenks.arcana.internal.spigot.anvil.AnvilMechanicsListener
import com.keopsfenks.arcana.internal.spigot.arrows.ArrowDataListener
import com.keopsfenks.arcana.internal.spigot.data.BlockDestroyListener
import com.keopsfenks.arcana.internal.spigot.data.DataYml
import com.keopsfenks.arcana.internal.spigot.data.PlayerBlockListener
import com.keopsfenks.arcana.internal.spigot.data.PlayerPlacedMarkers
import com.keopsfenks.arcana.internal.spigot.dragdrop.DragAndDropShellListener
import com.keopsfenks.arcana.internal.spigot.data.profiles.ProfileHandler
import com.keopsfenks.arcana.internal.spigot.data.profiles.ProfileLoadListener
import com.keopsfenks.arcana.internal.spigot.drops.CollatedRunnable
import com.keopsfenks.arcana.internal.spigot.entities.EntityArgParserNoClimb
import com.keopsfenks.arcana.internal.spigot.entities.SpiderClimbing
import com.keopsfenks.arcana.internal.spigot.eventlisteners.AutocrafterPatch
import com.keopsfenks.arcana.internal.spigot.eventlisteners.EntityDeathByEntityListeners
import com.keopsfenks.arcana.internal.spigot.eventlisteners.NaturalExpGainListenersPaper
import com.keopsfenks.arcana.internal.spigot.eventlisteners.NaturalExpGainListenersSpigot
import com.keopsfenks.arcana.internal.spigot.eventlisteners.PlayerHealthPatch
import com.keopsfenks.arcana.internal.spigot.eventlisteners.UnconsumablePatch
import com.keopsfenks.arcana.internal.spigot.eventlisteners.UnenchantablePatch
import com.keopsfenks.arcana.internal.spigot.eventlisteners.UngrindablePatch
import com.keopsfenks.arcana.internal.spigot.eventlisteners.PlayerJumpListenersPaper
import com.keopsfenks.arcana.internal.spigot.eventlisteners.PlayerJumpListenersSpigot
import com.keopsfenks.arcana.internal.spigot.eventlisteners.armor.ArmorChangeEventListeners
import com.keopsfenks.arcana.internal.spigot.eventlisteners.armor.ArmorListener
import com.keopsfenks.arcana.internal.spigot.eventlisteners.armor.ArmorListenerPaper
import com.keopsfenks.arcana.internal.spigot.gui.GUIListener
import com.keopsfenks.arcana.internal.spigot.gui.GUIQuitListener
import com.keopsfenks.arcana.internal.spigot.integrations.afk.AFKIntegrationCMI
import com.keopsfenks.arcana.internal.spigot.integrations.afk.AFKIntegrationEssentials
import com.keopsfenks.arcana.internal.spigot.integrations.anticheat.AnticheatSpartan
import com.keopsfenks.arcana.internal.spigot.integrations.anticheat.AnticheatVulcan
import com.keopsfenks.arcana.internal.spigot.integrations.antigrief.AntigriefBentoBox
import com.keopsfenks.arcana.internal.spigot.integrations.antigrief.AntigriefCombatLogX
import com.keopsfenks.arcana.internal.spigot.integrations.antigrief.AntigriefDeluxeCombat
import com.keopsfenks.arcana.internal.spigot.integrations.antigrief.AntigriefFabledSkyBlock
import com.keopsfenks.arcana.internal.spigot.integrations.antigrief.AntigriefFactionsUUID
import com.keopsfenks.arcana.internal.spigot.integrations.antigrief.AntigriefGriefPrevention
import com.keopsfenks.arcana.internal.spigot.integrations.antigrief.AntigriefHuskClaims
import com.keopsfenks.arcana.internal.spigot.integrations.antigrief.AntigriefHuskTowns
import com.keopsfenks.arcana.internal.spigot.integrations.antigrief.AntigriefIridiumSkyblock
import com.keopsfenks.arcana.internal.spigot.integrations.antigrief.AntigriefKingdoms
import com.keopsfenks.arcana.internal.spigot.integrations.antigrief.AntigriefLands
import com.keopsfenks.arcana.internal.spigot.integrations.antigrief.AntigriefPlotSquared
import com.keopsfenks.arcana.internal.spigot.integrations.antigrief.AntigriefPvPManager
import com.keopsfenks.arcana.internal.spigot.integrations.antigrief.AntigriefResidence
import com.keopsfenks.arcana.internal.spigot.integrations.antigrief.AntigriefRPGHorses
import com.keopsfenks.arcana.internal.spigot.integrations.antigrief.AntigriefSuperiorSkyblock2
import com.keopsfenks.arcana.internal.spigot.integrations.antigrief.AntigriefTowny
import com.keopsfenks.arcana.internal.spigot.integrations.antigrief.AntigriefWorldGuard
import com.keopsfenks.arcana.internal.spigot.integrations.customblocks.CustomBlocksCraftEngine
import com.keopsfenks.arcana.internal.spigot.integrations.customblocks.CustomBlocksItemsAdder
import com.keopsfenks.arcana.internal.spigot.integrations.customblocks.CustomBlocksNexo
import com.keopsfenks.arcana.internal.spigot.integrations.customblocks.CustomBlocksOraxen
import com.keopsfenks.arcana.internal.spigot.integrations.customentities.CustomEntitiesMythicMobs
import com.keopsfenks.arcana.internal.spigot.integrations.customitems.CustomItemsCraftEngine
import com.keopsfenks.arcana.internal.spigot.integrations.customitems.CustomItemsCustomCrafting
import com.keopsfenks.arcana.internal.spigot.integrations.customitems.CustomItemsDenizen
import com.keopsfenks.arcana.internal.spigot.integrations.customitems.CustomItemsExecutableItems
import com.keopsfenks.arcana.internal.spigot.integrations.customitems.CustomItemsHeadDatabase
import com.keopsfenks.arcana.internal.spigot.integrations.customitems.CustomItemsItemBridge
import com.keopsfenks.arcana.internal.spigot.integrations.customitems.CustomItemsItemsAdder
import com.keopsfenks.arcana.internal.spigot.integrations.customitems.CustomItemsMythicMobs
import com.keopsfenks.arcana.internal.spigot.integrations.customitems.CustomItemsNexo
import com.keopsfenks.arcana.internal.spigot.integrations.customitems.CustomItemsOraxen
import com.keopsfenks.arcana.internal.spigot.integrations.customrecipes.CustomRecipeCustomCrafting
import com.keopsfenks.arcana.internal.spigot.integrations.economy.EconomyVault
import com.keopsfenks.arcana.internal.spigot.integrations.mcmmo.McmmoIntegrationImpl
import com.keopsfenks.arcana.internal.spigot.integrations.model.EntityArgParserModel
import com.keopsfenks.arcana.internal.spigot.integrations.model.ModelIntegrationBetterModel
import com.keopsfenks.arcana.internal.spigot.integrations.model.ModelIntegrationModelEngine
import com.keopsfenks.arcana.internal.spigot.integrations.multiverseinventories.MultiverseInventoriesIntegration
import com.keopsfenks.arcana.internal.spigot.integrations.placeholder.PlaceholderIntegrationPAPI
import com.keopsfenks.arcana.internal.spigot.integrations.price.PriceFactoryCoinsEngine
import com.keopsfenks.arcana.internal.spigot.integrations.price.PriceFactoryPlayerPoints
import com.keopsfenks.arcana.internal.spigot.integrations.price.PriceFactoryRedisEconomy
import com.keopsfenks.arcana.internal.spigot.integrations.price.PriceFactoryRoyaleEconomy
import com.keopsfenks.arcana.internal.spigot.integrations.price.PriceFactoryUltraEconomy
import com.keopsfenks.arcana.internal.spigot.integrations.shop.ShopDeluxeSellwands
import com.keopsfenks.arcana.internal.spigot.integrations.shop.ShopEconomyShopGUI
import com.keopsfenks.arcana.internal.spigot.integrations.shop.ShopExcellentShop
import com.keopsfenks.arcana.internal.spigot.integrations.shop.ShopShopGuiPlus
import com.keopsfenks.arcana.internal.spigot.integrations.shop.ShopZShop
import com.keopsfenks.arcana.internal.spigot.proxies.FastItemStackFactoryProxy
import com.keopsfenks.arcana.internal.spigot.proxies.PacketHandlerProxy
import com.keopsfenks.arcana.internal.spigot.recipes.CraftingRecipeListener
import com.keopsfenks.arcana.internal.spigot.recipes.StackedRecipeListener
import com.keopsfenks.arcana.internal.spigot.recipes.listeners.ComplexInComplex
import com.keopsfenks.arcana.internal.spigot.recipes.listeners.ComplexInVanilla
import com.keopsfenks.arcana.internal.spigot.recipes.stackhandlers.ShapedCraftingRecipeStackHandler
import com.keopsfenks.arcana.internal.spigot.recipes.stackhandlers.ShapelessCraftingRecipeStackHandler
import com.keopsfenks.arcana.internal.spigot.recipes.workstation.BrewingPacketHandler
import com.keopsfenks.arcana.internal.spigot.recipes.workstation.GrindstonePacketHandler
import com.keopsfenks.arcana.internal.spigot.recipes.workstation.WorkstationRecipeListener
import com.keopsfenks.arcana.util.ClassUtils
import com.keopsfenks.arcana.util.PlayerUtils
import me.TechsCode.UltraEconomy.UltraEconomy
import dev.unnm3d.rediseconomy.api.RedisEconomyAPI
import me.qKing12.RoyaleEconomy.MultiCurrency.MultiCurrencyHandler
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.milkbowl.vault.economy.Economy
import com.keopsfenks.arcana.internal.spigot.datapack.BookkeepingLedgerStorage
import com.keopsfenks.arcana.internal.spigot.datapack.DatapackRegistry
import com.keopsfenks.arcana.internal.spigot.display.DisplayRefresher
import com.keopsfenks.arcana.internal.spigot.proxies.DatapackCodecProxy
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import su.nightexpress.excellenteconomy.api.ExcellentEconomyAPI

/**
 * The base arcana plugin, containing everything shared between platform implementations.
 */
abstract class ArcanaSpigotPlugin : ArcanaPlugin() {
    /**
     * data.yml, where arcana stores its internal data.
     */
    abstract val dataYml: DataYml

    /**
     * The handler responsible for loading and saving player profiles.
     */
    abstract val profileHandler: ProfileHandler

    /**
     * The adventure audience provider, only present on servers without native adventure support.
     */
    protected var bukkitAudiences: BukkitAudiences? = null

    /**
     * The brewing packet handler, registered as both a listener and a packet listener.
     */
    private val brewingPacketHandler = BrewingPacketHandler(this)

    /**
     * Sends displayed items to players again after reloads.
     */
    val displayRefresher = DisplayRefresher(this) { getProxy(PacketHandlerProxy::class.java).clearDisplayFrame(it) }

    /**
     * Picks and sends every player's persistent action bar.
     */
    val persistentActionBars = PersistentActionBarService(this.logger)

    private val persistentActionBarTicker = PersistentActionBarTicker(this, persistentActionBars)

    /**
     * Owns every plugin's datapack, and all the timing decisions around them.
     */
    val datapackRegistry: DatapackRegistry by lazy {
        DatapackRegistry(
            logger = this.logger,
            ledgerStorage = BookkeepingLedgerStorage(this.profileHandler.bookkeeping),
            proxyProvider = { this.getProxy(DatapackCodecProxy::class.java) }
        )
    }

    init {
        Items.registerArgParser(ArgParserEnchantment)
        Items.registerArgParser(ArgParserColor)
        Items.registerArgParser(ArgParserTexture)
        Items.registerArgParser(ArgParserPotionBuilder)
        Items.registerArgParser(ArgParserFireworkBuilder)
        Items.registerArgParser(ArgParserFireworkPower)
        Items.registerArgParser(ArgParserCustomModelData)
        Items.registerArgParser(ArgParserFlag)
        Items.registerArgParser(ArgParserUnbreakable)
        Items.registerArgParser(ArgParserName)
        Items.registerArgParser(ArgParserHead)
        Items.registerArgParser(ArgParserEntity)
        Items.registerArgParser(ArgParserFireResistant)
        Items.registerArgParser(ArgParserGlider)
        Items.registerArgParser(ArgParserGlint)
        Items.registerArgParser(ArgParserItemModel)
        Items.registerArgParser(ArgParserItemName)
        Items.registerArgParser(ArgParserMaxDamage)
        Items.registerArgParser(ArgParserMaxStackSize)
        Items.registerArgParser(ArgParserTooltipStyle)
        Items.registerArgParser(ArgParserTrim)
        Items.registerArgParser(ArgParserAttribute)
        Items.registerArgParser(ArgParserUnenchantable)
        Items.registerArgParser(ArgParserUngrindable)
        Items.registerArgParser(ArgParserUnconsumable)
        Items.registerArgParser(ArgParserUseCooldown)

        Blocks.registerArgParser(BlockArgParserAgeable)
        Blocks.registerArgParser(BlockArgParserAnaloguePowerable)
        Blocks.registerArgParser(BlockArgParserAttachable)
        Blocks.registerArgParser(BlockArgParserBamboo)
        Blocks.registerArgParser(BlockArgParserBisected)
        Blocks.registerArgParser(BlockArgParserBubbleColumn)
        Blocks.registerArgParser(BlockArgParserCake)
        Blocks.registerArgParser(BlockArgParserCampfire)
        Blocks.registerArgParser(BlockArgParserCaveVinesPlant)
        Blocks.registerArgParser(BlockArgParserChest)
        Blocks.registerArgParser(BlockArgParserCommandBlock)
        Blocks.registerArgParser(BlockArgParserComparator)
        Blocks.registerArgParser(BlockArgParserCrafter)
        Blocks.registerArgParser(BlockArgParserDaylightDetector)
        Blocks.registerArgParser(BlockArgParserDirectional)
        Blocks.registerArgParser(BlockArgParserDispenser)
        Blocks.registerArgParser(BlockArgParserDoor)
        Blocks.registerArgParser(BlockArgParserEndPortalFrame)
        Blocks.registerArgParser(BlockArgParserFaceAttachable)
        Blocks.registerArgParser(BlockArgParserFarmland)
        Blocks.registerArgParser(BlockArgParserGate)
        Blocks.registerArgParser(BlockArgParserHangable)
        Blocks.registerArgParser(BlockArgParserHangingMoss)
        Blocks.registerArgParser(BlockArgParserJigsaw)
        Blocks.registerArgParser(BlockArgParserLevelled)
        Blocks.registerArgParser(BlockArgParserLightable)
        Blocks.registerArgParser(BlockArgParserMossyCarpet)
        Blocks.registerArgParser(BlockArgParserMultipleFacing)
        Blocks.registerArgParser(BlockArgParserNoteBlock)
        Blocks.registerArgParser(BlockArgParserOpenable)
        Blocks.registerArgParser(BlockArgParserOrientable)
        Blocks.registerArgParser(BlockArgParserPointedDripstone)
        Blocks.registerArgParser(BlockArgParserPowerable)
        Blocks.registerArgParser(BlockArgParserQuantity)
        Blocks.registerArgParser(BlockArgParserRail)
        Blocks.registerArgParser(BlockArgParserRedstoneWire)
        Blocks.registerArgParser(BlockArgParserRepeater)
        Blocks.registerArgParser(BlockArgParserRespawnAnchor)
        Blocks.registerArgParser(BlockArgParserRotatable)
        Blocks.registerArgParser(BlockArgParserScaffolding)
        Blocks.registerArgParser(BlockArgParserSculkSensor)
        Blocks.registerArgParser(BlockArgParserSculkShrieker)
        Blocks.registerArgParser(BlockArgParserSlab)
        Blocks.registerArgParser(BlockArgParserSnow)
        Blocks.registerArgParser(BlockArgParserSnowy)
        Blocks.registerArgParser(BlockArgParserStairs)
        Blocks.registerArgParser(BlockArgParserStructureBlock)
        Blocks.registerArgParser(BlockArgParserTNT)
        Blocks.registerArgParser(BlockArgParserTrial)
        Blocks.registerArgParser(BlockArgParserTripwire)
        Blocks.registerArgParser(BlockArgParserWall)
        Blocks.registerArgParser(BlockArgParserWaterlogged)

        Entities.registerArgParser(EntityArgParserName)
        Entities.registerArgParser(EntityArgParserNoAI)
        Entities.registerArgParser(EntityArgParserAttackDamage)
        Entities.registerArgParser(EntityArgParserAttackSpeed)
        Entities.registerArgParser(EntityArgParserFlySpeed)
        Entities.registerArgParser(EntityArgParserFollowRange)
        Entities.registerArgParser(EntityArgParserHealth)
        Entities.registerArgParser(EntityArgParserKnockback)
        Entities.registerArgParser(EntityArgParserKnockbackResistance)
        Entities.registerArgParser(EntityArgParserSize)
        Entities.registerArgParser(EntityArgParserSpawnReinforcements)
        Entities.registerArgParser(EntityArgParserSpeed)
        Entities.registerArgParser(EntityArgParserBaby)
        Entities.registerArgParser(EntityArgParserAdult)
        Entities.registerArgParser(EntityArgParserTamed)
        Entities.registerArgParser(EntityArgParserCharged)
        Entities.registerArgParser(EntityArgParserExplosionRadius)
        Entities.registerArgParser(EntityArgParserSilent)
        Entities.registerArgParser(EntityArgParserEquipment)
        Entities.registerArgParser(EntityArgParserJumpStrength)
        Entities.registerArgParser(EntityArgParserScale)
        Entities.registerArgParser(EntityArgParserFirework)
        Entities.registerArgParser(EntityArgParserNoClimb)
        Entities.registerArgParser(EntityArgParserModel)

        Prices.registerDefaultPriceFactory(PriceFactoryEconomy)
        Prices.registerDefaultPriceFactory(PriceFactoryXPLevels)
        Prices.registerDefaultPriceFactory(PriceFactoryXP)

        Particles.registerParticleFactory(ParticleFactoryRGB)
        Particles.registerParticleFactory(ParticleFactoryDustTransition)
        Particles.registerParticleFactory(ParticleFactoryEntityEffect)

        CraftingRecipeListener.registerListener(ComplexInComplex)
        CraftingRecipeListener.registerListener(ComplexInVanilla)

        StackedRecipeListener.registerHandler(ShapedCraftingRecipeStackHandler)
        StackedRecipeListener.registerHandler(ShapelessCraftingRecipeStackHandler)

        SegmentParserGroup.register()
        SegmentParserUseIfPresent.register()

        CustomItemsManager.registerProviders()
        CustomBlocksManager.registerProviders()

        ExternalDataStore.registerAdapter(VersionToStringAdapter)
        // Handle with shadow.
        val className = listOf(
            "org",
            "apache",
            "maven",
            "artifact",
            "versioning",
            "DefaultArtifactVersion"
        ).joinToString(".")
        if (ClassUtils.exists(className)) {
            ExternalDataStore.registerAdapter(MavenVersionToStringAdapter(className))
        }

        // Register internal Discord webhook integration
        DiscordManager.register(DiscordIntegrationImpl(this))
    }

    /**
     * Warn about conflicting plugins, then enable everything that requires a running server.
     */
    override fun handleEnable() {
        // Before anything reads the key registry, most importantly profile migration in createTasks.
        PlayerUtils.registerDataKeys()

        this.logger.info("Scanning for conflicts...")
        val conflicts = ConflictFinder.searchForConflicts(this)
        for (conflict in conflicts) {
            this.logger.warning(conflict.conflictMessage)
        }
        if (conflicts.isNotEmpty()) {
            this.logger.warning(
                "You can fix the conflicts by either removing the conflicting plugins, " +
                        "or by asking on the support discord to have them patched!"
            )
            this.logger.warning(
                "Only remove potentially conflicting plugins if you see " +
                        "Loader Constraint Violation / LinkageError anywhere"
            )
        } else {
            this.logger.info("No conflicts found!")
        }

        // Do it again here
        CustomItemsManager.registerProviders()
        CustomBlocksManager.registerProviders()

        // Register events for ShopSellEvent
        for (integration in ShopManager.getRegisteredIntegrations()) {
            val listener = integration.sellEventAdapter
            if (listener != null) {
                this.eventManager.registerListener(listener)
            }
        }

        // Init FIS
        this.getProxy(FastItemStackFactoryProxy::class.java).create(ItemStack(Material.AIR)).unwrap()

        // Init adventure
        if (!Prerequisite.HAS_PAPER.isMet) {
            bukkitAudiences = BukkitAudiences.create(this)
        }

        // Init vanilla tags
        VanillaItemTags.register()
        VanillaBlockTags.register()
        VanillaEntityTags.register()

        // Worlds are loaded by now, so anything already published is committed to.
        this.datapackRegistry.onWorldsLoaded()
        this.datapackRegistry.handleOrphanedPacks(this.configYml.getBool("datapacks.remove-orphaned"))
    }

    /**
     * Shut down integrations and flush player data to storage.
     */
    override fun handleDisable() {
        DiscordManager.shutdown()
        this.logger.info("Saving player data...")
        val start = System.currentTimeMillis()
        profileHandler.save()
        this.logger.info("Saved player data! Took ${System.currentTimeMillis() - start}ms")
        Arcana.get().adventure?.close()
    }

    /**
     * Start the collated runnable, and check recipe batching if no profile migration is pending.
     */
    override fun createTasks() {
        CollatedRunnable(this)

        persistentActionBarTicker.startAll()

        if (!profileHandler.migrateIfNecessary()) {
            profileHandler.profileWriter.startTickingAutosave()
            profileHandler.profileWriter.startTickingSaves()
            profileHandler.startSyncIfEnabled()
            profileHandler.backfillIfNecessary()
        }

        this.scheduler.global().runTimer(
            this.configYml.getInt("display-frame-ttl").toLong(),
            this.configYml.getInt("display-frame-ttl").toLong(),
        ) { getProxy(PacketHandlerProxy::class.java).clearDisplayFrames() }

        this.scheduler.global().runTimer(1L, 20L) {
            Recipes.checkBatching()
        }

        val refreshInterval = this.configYml.getInt("display-refresh-interval").toLong()

        if (refreshInterval > 0) {
            displayRefresher.startPeriodicRefresh(refreshInterval)
        }
    }

    /**
     * Register custom content from other plugins, once every plugin has loaded.
     */
    override fun handleAfterLoad() {
        CustomItemsManager.registerAllItems()
        CustomBlocksManager.registerAllBlocks()
        CustomEntitiesManager.registerAllEntities()
        ShopManager.registerArcanaProvider()
    }

    /**
     * Get the loaders for every plugin arcana integrates with.
     *
     * @return The integration loaders.
     */
    override fun loadIntegrationLoaders(): List<IntegrationLoader> {
        return listOf(
            // AntiGrief
            IntegrationLoader("IridiumSkyblock") { AntigriefManager.register(AntigriefIridiumSkyblock()) },
            IntegrationLoader("DeluxeCombat") { AntigriefManager.register(AntigriefDeluxeCombat()) },
            IntegrationLoader("SuperiorSkyblock2") { AntigriefManager.register(AntigriefSuperiorSkyblock2()) },
            IntegrationLoader("BentoBox") { AntigriefManager.register(AntigriefBentoBox()) },
            IntegrationLoader("WorldGuard") { AntigriefManager.register(AntigriefWorldGuard()) },
            IntegrationLoader("GriefPrevention") { AntigriefManager.register(AntigriefGriefPrevention()) },
            IntegrationLoader("FactionsUUID") { AntigriefManager.register(AntigriefFactionsUUID()) },
            IntegrationLoader("Towny") { AntigriefManager.register(AntigriefTowny()) },
            IntegrationLoader("Lands") { AntigriefManager.register(AntigriefLands(this)) },
            IntegrationLoader("Kingdoms") { AntigriefManager.register(AntigriefKingdoms()) },
            IntegrationLoader("RPGHorses") { AntigriefManager.register(AntigriefRPGHorses()) },
            IntegrationLoader("Residence") { AntigriefManager.register(AntigriefResidence()) },
            IntegrationLoader("HuskTowns") { AntigriefManager.register(AntigriefHuskTowns()) },
            IntegrationLoader("HuskClaims") { AntigriefManager.register(AntigriefHuskClaims()) },
            IntegrationLoader("CombatLogX") { AntigriefManager.register(AntigriefCombatLogX()) },
            IntegrationLoader("PvPManager") { AntigriefManager.register(AntigriefPvPManager()) },
            IntegrationLoader("FabledSkyblock") { AntigriefManager.register(AntigriefFabledSkyBlock()) },
            IntegrationLoader("PlotSquared") { AntigriefManager.register(AntigriefPlotSquared()) },

            // Anticheat
            IntegrationLoader("Spartan") { AnticheatManager.register(AnticheatSpartan()) },
            IntegrationLoader("Vulcan") { AnticheatManager.register(AnticheatVulcan()) },

            // Custom Entities
            IntegrationLoader("MythicMobs") { CustomEntitiesManager.register(CustomEntitiesMythicMobs()) },

            // Custom Items
            IntegrationLoader("Oraxen") {
                CustomItemsManager.register(CustomItemsOraxen(this))
                CustomBlocksManager.register(CustomBlocksOraxen(this))
            },
            IntegrationLoader("Nexo") {
                CustomItemsManager.register(CustomItemsNexo(this))
                CustomBlocksManager.register(CustomBlocksNexo(this))
            },
            IntegrationLoader("ItemsAdder") {
                CustomItemsManager.register(CustomItemsItemsAdder())
                CustomBlocksManager.register(CustomBlocksItemsAdder())
            },
            IntegrationLoader("CraftEngine") {
                CustomItemsManager.register(CustomItemsCraftEngine(this))
                CustomBlocksManager.register(CustomBlocksCraftEngine(this))
            },
            IntegrationLoader("HeadDatabase") { CustomItemsManager.register(CustomItemsHeadDatabase(this)) },
            IntegrationLoader("ExecutableItems") { CustomItemsManager.register(CustomItemsExecutableItems()) },
            IntegrationLoader("CustomCrafting") {
                CustomItemsManager.register(CustomItemsCustomCrafting())
                CraftingRecipeListener.registerValidator(CustomRecipeCustomCrafting())
            },
            IntegrationLoader("MythicMobs") { CustomItemsManager.register(CustomItemsMythicMobs(this)) },
            IntegrationLoader("Denizen") { CustomItemsManager.register(CustomItemsDenizen()) },
            IntegrationLoader("ItemBridge") { CustomItemsManager.register(CustomItemsItemBridge()) },

            // Shop
            IntegrationLoader("ShopGUIPlus") { ShopManager.register(ShopShopGuiPlus()) },
            IntegrationLoader("zShop") { ShopManager.register(ShopZShop()) },
            IntegrationLoader("DeluxeSellwands") { ShopManager.register(ShopDeluxeSellwands()) },
            IntegrationLoader("EconomyShopGUI") { ShopManager.register(ShopEconomyShopGUI()) },
            IntegrationLoader("EconomyShopGUI-Premium") { ShopManager.register(ShopEconomyShopGUI()) },
            IntegrationLoader("ExcellentShop") { ShopManager.register(ShopExcellentShop()) },

            // AFK
            IntegrationLoader("Essentials") { AFKManager.register(AFKIntegrationEssentials()) },
            IntegrationLoader("CMI") { AFKManager.register(AFKIntegrationCMI()) },

            // Economy
            IntegrationLoader("Vault") {
                val rsp = Bukkit.getServer().servicesManager.getRegistration(Economy::class.java)
                if (rsp != null) {
                    EconomyManager.register(EconomyVault(rsp.provider))
                }
            },

            // Price
            IntegrationLoader("UltraEconomy") {
                for (currency in UltraEconomy.getAPI().currencies) {
                    registerCurrency(PriceFactoryUltraEconomy(currency))
                }
            },
            IntegrationLoader("PlayerPoints") { Prices.registerPriceFactory(PriceFactoryPlayerPoints()) },
            IntegrationLoader("RoyaleEconomy") {
                if (!MultiCurrencyHandler.getCurrencies().isNullOrEmpty()) {
                    for (currency in MultiCurrencyHandler.getCurrencies()) {
                        registerCurrency(PriceFactoryRoyaleEconomy(currency))
                    }
                }
            },
            IntegrationLoader("ExcellentEconomy") {
                val rsp = Bukkit.getServer().servicesManager.getRegistration(ExcellentEconomyAPI::class.java)
                if (rsp != null) {
                    val api = rsp.provider
                    for (currency in api.currencies) {
                        registerCurrency(PriceFactoryCoinsEngine(api, currency))
                    }
                }
            },
            IntegrationLoader("RedisEconomy") {
                val api = RedisEconomyAPI.getAPI()
                if (api != null) {
                    for (currency in api.currencies) {
                        registerCurrency(PriceFactoryRedisEconomy(currency))
                    }
                }
            },

            // Placeholder
            IntegrationLoader("PlaceholderAPI") { PlaceholderManager.addIntegration(PlaceholderIntegrationPAPI()) },

            // Misc
            IntegrationLoader("mcMMO") { McmmoManager.register(McmmoIntegrationImpl()) },
            IntegrationLoader("Multiverse-Inventories") {
                this.eventManager.registerListener(
                    MultiverseInventoriesIntegration(this)
                )
            },
            IntegrationLoader("ModelEngine") { ModelManager.register(ModelIntegrationModelEngine()) },
            IntegrationLoader("BetterModel") { ModelManager.register(ModelIntegrationBetterModel()) }
        )
    }

    /**
     * Register a currency price factory, skipping it if its name is already taken by another
     * factory, so that one clashing currency doesn't stop the rest from loading.
     */
    private fun registerCurrency(factory: PriceFactory) {
        try {
            Prices.registerPriceFactory(factory)
        } catch (e: IllegalStateException) {
            this.logger.warning(e.message)
        }
    }

    /**
     * Get the listeners to register, including the paper or spigot specific variants.
     *
     * @return The listeners.
     */
    override fun loadListeners(): List<Listener> {
        val playerPlacedMarkers = PlayerPlacedMarkers(this)

        val listeners = mutableListOf(
            AnvilMechanicsListener(this),
            DragAndDropShellListener(),
            ArmorListener(),
            EntityDeathByEntityListeners(this),
            CraftingRecipeListener(this),
            StackedRecipeListener(this),
            WorkstationRecipeListener(this),
            brewingPacketHandler,
            GUIListener(this),
            GUIQuitListener,
            ArrowDataListener(this),
            ArmorChangeEventListeners(this),
            ProfileLoadListener(this, profileHandler),
            PlayerBlockListener(this, playerPlacedMarkers),
            BlockDestroyListener(playerPlacedMarkers),
            ServerLocking,
            OutdatedPlugins,
            AutocrafterPatch,
            PlayerHealthPatch,
            UnenchantablePatch,
            UngrindablePatch,
            SpiderClimbing,
            UnconsumablePatch,
            persistentActionBarTicker
        )

        if (Prerequisite.HAS_PAPER.isMet) {
            listeners.add(PlayerJumpListenersPaper())
            listeners.add(NaturalExpGainListenersPaper())
            listeners.add(ArmorListenerPaper())
        } else {
            listeners.add(PlayerJumpListenersSpigot())
            listeners.add(NaturalExpGainListenersSpigot())
        }

        return listeners
    }

    /**
     * Get the packet listeners to register, including those provided by the NMS proxy.
     *
     * @return The packet listeners.
     */
    override fun loadPacketListeners(): List<PacketListener> {
        return this.getProxy(PacketHandlerProxy::class.java).getPacketListeners(this) +
            listOf(brewingPacketHandler, GrindstonePacketHandler(this))
    }
}
