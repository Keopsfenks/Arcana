@file:JvmName("PlaceholderExtensions")

package com.keopsfenks.arcana.core.placeholder

import com.keopsfenks.arcana.core.integrations.placeholder.PlaceholderManager
import com.keopsfenks.arcana.core.placeholder.context.PlaceholderContext

/** @see PlaceholderManager.translatePlaceholders */
fun String.translatePlaceholders(context: PlaceholderContext) =
    PlaceholderManager.translatePlaceholders(this, context)

/** @see PlaceholderManager.findPlaceholdersIn */
fun String.findPlaceholders(): List<String> =
    PlaceholderManager.findPlaceholdersIn(this)
