# Add project specific ProGuard rules here.
-keep class com.careerlens.app.data.** { *; }
-keep class com.careerlens.app.domain.model.** { *; }
-keep class com.careerlens.app.domain.ats.** { *; }
-keep class ai.onnxruntime.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn ai.onnxruntime.**
