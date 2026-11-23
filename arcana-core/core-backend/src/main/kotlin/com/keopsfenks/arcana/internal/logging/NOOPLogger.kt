package com.keopsfenks.arcana.internal.logging

import java.util.logging.LogRecord
import java.util.logging.Logger

object NOOPLogger : Logger("arcana_noop", null as String?) {
    override fun log(record: LogRecord?) {
        return
    }
}
