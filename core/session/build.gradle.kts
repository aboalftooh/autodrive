plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}
android {
    namespace = "com.autodrive.app.core.session"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    api(project(":core:model"))
    implementation(libs.androidx.security.crypto); implementation(libs.datastore.preferences)
    implementation(libs.coroutines.android); implementation(libs.hilt.android); ksp(libs.hilt.compiler)
}
