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

# WorkManager is pulled in by Glance to manage background widget updates and opens
# a Room database at app startup.
# Room resolves its generated implementation by name, which R8 cannot see, so both
# the base class and the generated _Impl must survive shrinking and renaming.
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>(android.content.Context, java.lang.String, androidx.room.RoomDatabase$Callback);
}

# WorkManager reflection rules under R8 full mode.
# WorkManager instantiates InputMergers by reflection using zero-arg constructor.
-keep class * extends androidx.work.InputMerger {
    <init>();
}
# WorkManager instantiates Workers and ListenableWorkers by reflection.
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.work.impl.** { *; }

# Keep source/lines out of stack traces only; keep attributes needed by the platform.
-keepattributes *Annotation*, InnerClasses, Signature, SourceFile, LineNumberTable
