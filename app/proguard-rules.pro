# Glance serializes ActionCallback class names into PendingIntents and instantiates
# them reflectively, so R8 must not rename or strip them.
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    <init>();
}
-keep class in.rahulja.vision202020.widget.** { *; }

# Entry points referenced from AndroidManifest at runtime.
-keep class in.rahulja.vision202020.service.VisionTimerService { *; }
-keep class in.rahulja.vision202020.tile.VisionTileService { *; }
-keep class in.rahulja.vision202020.service.BootReceiver { *; }

# Keep source/lines out of stack traces only; keep attributes needed by the platform.
-keepattributes *Annotation*, InnerClasses, Signature, SourceFile, LineNumberTable
