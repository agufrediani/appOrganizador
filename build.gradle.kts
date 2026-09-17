val userTemp = System.getenv("TEMP") ?: "C:/Users/Agu/AppData/Local/Temp"
System.setProperty("java.io.tmpdir", userTemp)
System.setProperty("org.sqlite.tmpdir", userTemp)

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.ksp) apply false
}