# R8 / ProGuard rules for the release build.
# Hilt, Room, Compose, and coroutines ship their own consumer rules; these are
# small, defensive additions.

# Keep enum valueOf/values (PinCategory is looked up by name from the DB).
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Room entities and generated implementations.
-keep class com.prempoddar.loadstone.data.db.** { *; }

# Kotlin metadata (helps reflection-free libraries behave under shrinking).
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
