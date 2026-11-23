package com.keopsfenks.arcana.internal.spigot.proxy.v1_21_8

import com.keopsfenks.arcana.core.Prerequisite
import com.keopsfenks.arcana.core.packet.Packet
import com.keopsfenks.arcana.core.packet.sendPacket
import com.keopsfenks.arcana.internal.spigot.proxies.DisplayNameProxy
import com.keopsfenks.arcana.internal.spigot.proxy.common.toNMS
import net.minecraft.network.chat.Component as NMSComponent
import java.util.Optional
import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.Entity
import org.bukkit.craftbukkit.entity.CraftLivingEntity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player

@Suppress("UNCHECKED_CAST")
class DisplayName : DisplayNameProxy {
    private val displayNameAccessor = Entity::class.java
        .declaredFields
        .filter { it.type == EntityDataAccessor::class.java }
        .toList()[2]
        .apply { isAccessible = true }
        .get(null) as EntityDataAccessor<Optional<NMSComponent>>

    private val customNameVisibleAccessor = Entity::class.java
        .declaredFields
        .filter { it.type == EntityDataAccessor::class.java }
        .toList()[3]
        .apply { isAccessible = true }
        .get(null) as EntityDataAccessor<Boolean>

    override fun setClientsideDisplayName(
        entity: LivingEntity,
        player: Player,
        displayName: Component,
        visible: Boolean
    ) {
        if (entity !is CraftLivingEntity) {
            return
        }

        if (!Prerequisite.HAS_PAPER.isMet) {
            return
        }

        val nmsComponent = displayName.toNMS()

        val nmsEntity = entity.handle

        val packet = ClientboundSetEntityDataPacket(
            nmsEntity.id,
            listOf(
                SynchedEntityData.DataValue.create(displayNameAccessor, Optional.of(nmsComponent)),
                SynchedEntityData.DataValue.create(customNameVisibleAccessor, visible)
            )
        )

        player.sendPacket(Packet(packet))
    }
}
