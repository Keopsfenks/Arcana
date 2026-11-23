package com.keopsfenks.arcana.internal.spigot.integrations.bstats

import com.keopsfenks.arcana.core.ArcanaPlugin
import com.keopsfenks.arcana.core.bstats.ArcanaMetricsChart
import com.keopsfenks.arcana.libs.bstats.bukkit.Metrics
import com.keopsfenks.arcana.libs.bstats.charts.AdvancedBarChart
import com.keopsfenks.arcana.libs.bstats.charts.AdvancedPie
import com.keopsfenks.arcana.libs.bstats.charts.CustomChart
import com.keopsfenks.arcana.libs.bstats.charts.DrilldownPie
import com.keopsfenks.arcana.libs.bstats.charts.MultiLineChart
import com.keopsfenks.arcana.libs.bstats.charts.SimpleBarChart
import com.keopsfenks.arcana.libs.bstats.charts.SimplePie
import com.keopsfenks.arcana.libs.bstats.charts.SingleLineChart
import java.util.Properties

object MetricHandler {
    fun createMetrics(plugin: ArcanaPlugin) {
        val metrics = Metrics(plugin, plugin.bStatsId)

        for (chart in plugin.customCharts) {
            metrics.addCustomChart(chart.toBStatsChart())
        }

        metrics.addCustomChart(
            AdvancedPie("integrations_active") {
                plugin.loadedIntegrations
                    .associateWith { 1 }
                    .ifEmpty { null }
            }
        )

        metrics.addCustomChart(
            SimplePie("platform") {
                plugin.getPublishPlatform().displayName
            }
        )
    }

    private fun ArcanaPlugin.getPublishPlatform(): PublishPlatform {
        val props = Properties()

        val label = try {
            this.javaClass.getResourceAsStream("/platform")?.use { props.load(it) }
            props.getProperty("platform")
        } catch (e: Exception) {
            null
        } ?: return PublishPlatform.OTHER

        return PublishPlatform.fromMarkerLabel(label)
    }

    private fun ArcanaMetricsChart.toBStatsChart(): CustomChart = when (this) {
        is ArcanaMetricsChart.SimplePie -> SimplePie(id) { supplier() }
        is ArcanaMetricsChart.AdvancedPie -> AdvancedPie(id) { supplier() }
        is ArcanaMetricsChart.DrilldownPie -> DrilldownPie(id) { supplier() }
        is ArcanaMetricsChart.SingleLine -> SingleLineChart(id) { supplier() }
        is ArcanaMetricsChart.MultiLine -> MultiLineChart(id) { supplier() }
        is ArcanaMetricsChart.SimpleBar -> SimpleBarChart(id) { supplier() }
        is ArcanaMetricsChart.AdvancedBar -> AdvancedBarChart(id) { supplier() }
    }
}
