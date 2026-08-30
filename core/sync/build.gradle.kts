plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}
android {
    namespace = "com.autodrive.app.core.sync"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    api(project(":core:database")); api(project(":core:network")); api(project(":core:session")); api(project(":core:observability"))
    implementation(libs.androidx.core.ktx); implementation(libs.coroutines.android)
    implementation(libs.work.runtime.ktx); implementation(libs.hilt.work); ksp(libs.hilt.work.compiler)
    implementation(platform(libs.supabase.bom)); implementation(libs.supabase.postgrest); implementation(libs.supabase.auth); implementation(libs.supabase.realtime)
    implementation(libs.hilt.android); ksp(libs.hilt.compiler)
}
