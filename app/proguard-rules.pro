# androidsvg
-keep class com.caverock.androidsvg.** { *; }
-dontwarn com.caverock.androidsvg.**

# data classes (Config)
-keep class ir.apkbuilder.** { *; }

# kotlinx serialization / json
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
