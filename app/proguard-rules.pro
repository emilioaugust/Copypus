# 1. Glance widget and its receivers/actions
-keep class com.emilioaugust.copypus.widget.** { *; }

# 2. Room database and generated implementation
-keep class com.emilioaugust.copypus.data.database.AppDatabase { *; }

# 3. DAO and generated implementation
-keep interface com.emilioaugust.copypus.data.dao.ClipboardDao { *; }

# 4. Entity used by the widget
-keep class com.emilioaugust.copypus.data.entity.ClipboardItem { *; }

# 5. Repository used by the widget
-keep class com.emilioaugust.copypus.data.repository.ClipboardRepository { *; }

# Keep WorkManager's default InputMerger
-keep class androidx.work.OverwritingInputMerger {
    public <init>();
}

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod