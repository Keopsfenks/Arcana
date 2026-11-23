@file:JvmName("RegistryExtensions")

package com.keopsfenks.arcana.core.registry

/** @see Registry.tryFitPattern */
fun String.tryFitRegistryPattern(): String =
    Registry.tryFitPattern(this)
