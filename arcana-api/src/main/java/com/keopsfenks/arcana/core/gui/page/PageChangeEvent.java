package com.keopsfenks.arcana.core.gui.page;

import com.keopsfenks.arcana.core.gui.menu.MenuEvent;

/**
 * Represents a page change.
 *
 * @param newPage The new page.
 * @param oldPage The old page.
 */
public record PageChangeEvent(
        int newPage,
        int oldPage
) implements MenuEvent {

}
