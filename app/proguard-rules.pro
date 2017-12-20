-dontshrink
-dontpreverify
-dontusemixedcaseclassnames

-flattenpackagehierarchy
-allowaccessmodification

-optimizationpasses 7
-verbose

# Optimization is turned off by default. Dex does not like code run
# through the ProGuard optimize and preverify steps (and performs some
# of these optimizations on its own).
-dontoptimize
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-ignorewarnings

-keepattributes InnerClasses
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile
-keepattributes LineNumberTable

-keep public class com.google.vending.licensing.ILicensingService
-keep public class com.android.vending.licensing.ILicensingService
-keep class * extends java.lang.annotation.Annotation

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.preference.PreferenceActivity
-keep public class * extends java.lang.Throwable {*;}
-keep public class * extends java.lang.Exception {*;}

# For native methods, see http://proguard.sourceforge.net/manual/examples.html#native
-keepclasseswithmembernames class * {
    native <methods>;
}

# keep setters in Views so that animations can still work.
# see http://proguard.sourceforge.net/manual/examples.html#beans
-keepclassmembers public class * extends android.view.View {
   void set*(***);
   *** get*();
}

# We want to keep methods in Activity that could be used in the XML attribute onClick
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

# For enumeration classes, see http://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class **.R$* {
    public static <fields>;
}

# The support library contains references to newer platform versions.
# Don't warn about those in case this app is linking against an older
# platform version.  We know about them, and they are safe.
-dontwarn android.support.**

-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

# webview
-keep public class android.webkit.** {*;}

# keep openFileChooser
-keepclassmembers class * {
    private static final int FILECHOOSER_RESULTCODE;
    public void openFileChooser(android.webkit.ValueCallback,java.lang.String,java.lang.String);
    public void openFileChooser(android.webkit.ValueCallback,java.lang.String);
    public void openFileChooser(android.webkit.ValueCallback);
}

-dontwarn android.support.v4.**
-dontwarn android.support.v7.**
-dontwarn android.support.v13.**

-keep public class android.support.v4.** { *; }
-keep interface android.support.v4.app.** { *; }
-keep public class * extends android.support.v4.**

-keep public class android.support.v7.** { *; }
-keep interface android.support.v7.app.** { *; }
-keep public class * extends android.support.v7.**

-keep public class android.support.v13.** { *; }
-keep interface android.support.v13.app.** { *; }
-keep public class * extends android.support.v13.**

-keep public class * extends android.app.Fragment
-keep public class * extends android.support.v4.app.Fragment
-keep public class android.webkit.** { *; }

-dontwarn org.apache.http.**
-keep class android.net.http.** { *; }
-dontwarn android.net.http.**
-dontwarn android.webkit.WebViewClient

-dontwarn android.net.http.SslError
-keep public class android.net.http.SslError{*;}

-dontwarn com.squareup.okhttp.**
-keep class com.squareup.okhttp.** { *; }

-dontwarn org.apache.http.**
-keep class org.apache.http.** { *; }

-keep public class android.webkit.WebViewClient{*;}
-keep public class android.webkit.WebChromeClient{*;}
-keep public interface android.webkit.WebChromeClient$CustomViewCallback {*;}
-keep public interface android.webkit.ValueCallback {*;}
-keep class * implements android.webkit.WebChromeClient {*;}

-keep public class * extends android.app.Activity{
  public protected *;
}

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# adding this in to preserve line numbers so that the stack traces
# can be remapped
-renamesourcefileattribute SourceFile

-keep class * extends android.app.Activity{*;}
-keep class * extends android.app.Service{*;}

-keep class com.zf.sync.Main {*;}