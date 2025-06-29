-keep class * {
    @com.shalan.analytics.annotation.TrackScreen <fields>;
}
-keep @com.shalan.analytics.annotation.TrackScreen class * { *; }
-keepattributes *Annotation*
