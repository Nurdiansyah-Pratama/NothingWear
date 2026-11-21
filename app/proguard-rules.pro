# NothingWear Release R8 / ProGuard Rules (M7)
# Keep domain models serialized/inspected via reflection or Room/WorkManager/Health Connect.

-keep class com.nothingwear.app.core.database.entity.** { *; }
-keep class com.nothingwear.app.core.database.dao.** { *; }
-keep class com.nothingwear.app.data.event.CalendarReminderWorker { *; }
-keep class com.nothingwear.app.widget.NothingWearWidgetProvider { *; }

# Preserve line numbers in crash stack traces (no secrets or PII)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
