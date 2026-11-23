# Preserve metadata used by annotations and reflection.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Preserve source line information for release crash diagnostics.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** {
    *;
}
