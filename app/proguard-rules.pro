# Reglas de ofuscación y preservación R8 para librerías de red SMB (SMBJ, JCIFS-NG) y FTP (Apache Commons Net)
-keep class com.hierynomus.** { *; }
-keep class eu.agno3.jcifs.** { *; }
-keep class jcifs.** { *; }
-keep class org.apache.commons.net.** { *; }
-keep class org.bouncycastle.** { *; }

-dontwarn com.hierynomus.**
-dontwarn eu.agno3.jcifs.**
-dontwarn jcifs.**
-dontwarn org.apache.commons.net.**
-dontwarn org.bouncycastle.**
-dontwarn javax.naming.**
