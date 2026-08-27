# WebView <-> JS köprüsü refleksiyonla çağrılır; @JavascriptInterface üyelerini koru.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface, *Annotation*
