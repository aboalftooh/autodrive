plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
android {
    namespace = "com.autodrive.app.core.database"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    api(libs.room.runtime)
    api(libs.room.ktx)
    api(libs.coroutines.android)
    ksp(libs.room.compiler)
}
