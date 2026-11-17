package com.vision202020.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.GlanceAppWidget

class VisionWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = object : GlanceAppWidget() {
        override suspend fun provideGlance(context: Context, id: GlanceId) {
            // Placeholder for Task 6
        }
    }
}
