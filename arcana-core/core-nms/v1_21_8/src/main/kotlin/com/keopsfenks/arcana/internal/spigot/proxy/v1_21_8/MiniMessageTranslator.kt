package com.keopsfenks.arcana.internal.spigot.proxy.v1_21_8

import com.keopsfenks.arcana.core.display.Display
import com.keopsfenks.arcana.internal.spigot.proxies.MiniMessageTranslatorProxy
import com.keopsfenks.arcana.util.toLegacy
import net.kyori.adventure.text.minimessage.MiniMessage

class MiniMessageTranslator : MiniMessageTranslatorProxy {
    @Suppress("DEPRECATION")
    override fun format(message: String): String {
        var mut = message

        val startsWithPrefix = mut.startsWith(Display.PREFIX)
        if (startsWithPrefix) {
            mut = mut.substring(2)
        }

        mut = mut.replace('§', '&')

        val miniMessage = runCatching {
            MiniMessage.miniMessage().deserialize(
                mut
            ).toLegacy()
        }.getOrNull() ?: mut

        mut = if (startsWithPrefix) {
            Display.PREFIX + miniMessage
        } else {
            miniMessage
        }

        return mut
    }
}
