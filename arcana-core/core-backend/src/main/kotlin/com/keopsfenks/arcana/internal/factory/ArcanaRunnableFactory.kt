package com.keopsfenks.arcana.internal.factory

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.factory.RunnableFactory
import com.keopsfenks.arcana.core.scheduling.RunnableTask
import com.keopsfenks.arcana.internal.scheduling.ArcanaRunnableTask
import java.util.function.Consumer

class ArcanaRunnableFactory(private val plugin: ArcanaPlugin) : RunnableFactory {
    override fun create(consumer: Consumer<RunnableTask>): RunnableTask {
        return object : ArcanaRunnableTask(plugin) {
            override fun run() {
                consumer.accept(this)
            }
        }
    }
}