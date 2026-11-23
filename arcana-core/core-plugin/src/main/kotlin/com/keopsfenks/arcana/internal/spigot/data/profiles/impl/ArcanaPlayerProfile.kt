package com.keopsfenks.arcana.internal.spigot.data.profiles.impl

import com.keopsfenks.arcana.core.data.PlayerProfile
import com.keopsfenks.arcana.internal.spigot.data.profiles.ProfileHandler
import java.util.UUID

class ArcanaPlayerProfile(
    uuid: UUID,
    handler: ProfileHandler
) : ArcanaProfile(uuid, handler), PlayerProfile {
    override fun toString(): String {
        return "ArcanaPlayerProfile{uuid=$uuid}"
    }
}
