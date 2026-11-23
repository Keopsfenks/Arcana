package com.keopsfenks.arcana.internal.lookup

import com.keopsfenks.arcana.core.lookup.LookupHandler
import com.keopsfenks.arcana.core.lookup.SegmentParser
import com.keopsfenks.arcana.core.lookup.Testable

object SegmentParserUseIfPresent : SegmentParser("?") {
    override fun <T : Testable<*>> handleSegments(segments: Array<out String>, handler: LookupHandler<T>): T {
        for (option in segments) {
            val lookup = handler.parseKey(option)
            if (handler.validate(lookup)) {
                return lookup
            }
        }

        return handler.failsafe
    }
}
