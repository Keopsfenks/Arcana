package com.keopsfenks.arcana.util;

import com.keopsfenks.arcana.core.Arcana;

/**
 * Utilities / API methods for the server.
 */
public final class ServerUtils {
    /**
     * Get the current server TPS.
     * <p>
     * The value is capped at 20, so a server running above nominal speed still reports 20.
     *
     * @return The TPS, at most 20.
     */
    public static double getTps() {
        double tps = Arcana.get().getTPS();

        if (tps > 20) {
            return 20;
        } else {
            return tps;
        }
    }

    private ServerUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
