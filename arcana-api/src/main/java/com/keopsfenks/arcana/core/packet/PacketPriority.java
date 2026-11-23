package com.keopsfenks.arcana.core.packet;

/**
 * The priority (order) of packet listeners.
 */
public enum PacketPriority {
    /**
     * Ran first.
     */
    LOWEST,

    /**
     * Ran second.
     */
    LOW,

    /**
     * Ran third.
     */
    NORMAL,

    /**
     * Ran fourth.
     */
    HIGH,

    /**
     * Ran last.
     */
    HIGHEST
}
