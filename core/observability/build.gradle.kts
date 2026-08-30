plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}
android {
    namespace = "com.autodrive.app.core.observability"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    buildTypes {
        debug { buildConfigField("String", "ENVIRONMENT", "\"debug\""); buildConfigField("boolean", "CRASH_REPORTING_ENABLED", "false") }
        release { buildConfigField("String", "ENVIRONMENT", "\"production\""); buildConfigField("boolean", "CRASH_REPORTING_ENABLED", "true") }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { buildConfig = true }
}
dependencies {
    implementation(platform(libs.firebase.bom)); implementation(libs.firebase.crashlytics)
    implementation(libs.hilt.android); ksp(libs.hilt.compiler)
}
