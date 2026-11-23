@file:JvmName("TestableExtensions")

package com.keopsfenks.arcana.core.lookup

/** @see Testable.matches */
fun <T> T?.matches(test: Testable<T>) =
    test.matches(this)
