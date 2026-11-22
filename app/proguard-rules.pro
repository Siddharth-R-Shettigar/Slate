# Keep Room database, entities, and DAOs intact under R8 minification
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * {
    <fields>;
    <init>(...);
}
-keep @androidx.room.Dao interface *

# Keep DataStore preferences enum names used via valueOf/entries
-keepclassmembers enum com.example.data.preferences.AppColorTheme {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
