package com.keopsfenks.arcana.internal.spigot.data.profiles.impl

import com.keopsfenks.arcana.core.data.ServerProfile
import com.keopsfenks.arcana.core.data.keys.PersistentDataKey
import com.keopsfenks.arcana.core.data.keys.PersistentDataKeyType
import com.keopsfenks.arcana.internal.spigot.data.profiles.ProfileHandler
import com.keopsfenks.arcana.util.namespacedKeyOf
import java.util.UUID

val serverIDKey = PersistentDataKey(
    namespacedKeyOf("arcana", "server_id"),
    PersistentDataKeyType.STRING,
    ""
)

val localServerIDKey = PersistentDataKey(
    namespacedKeyOf("arcana", "local_server_id"),
    PersistentDataKeyType.STRING,
    "",
    true
)

val serverProfileUUID = UUID(0, 0)

class ArcanaServerProfile(
    handler: ProfileHandler
) : ArcanaProfile(serverProfileUUID, handler), ServerProfile {
    override fun getServerID(): String {
        if (this.read(serverIDKey).isBlank()) {
            this.write(serverIDKey, UUID.randomUUID().toString())
        }

        return this.read(serverIDKey)
    }

    override fun getLocalServerID(): String {
        if (this.read(localServerIDKey).isBlank()) {
            this.write(localServerIDKey, UUID.randomUUID().toString())
        }

        return this.read(localServerIDKey)
    }

    override fun toString(): String {
        return "ArcanaServerProfile"
    }
}
