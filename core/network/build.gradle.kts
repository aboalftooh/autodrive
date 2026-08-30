import java.util.Properties
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}
fun configurationValue(name: String, legacyName: String? = null): String =
    providers.gradleProperty(name).orNull ?: providers.environmentVariable(name).orNull
    ?: localProperties.getProperty(name) ?: legacyName?.let(localProperties::getProperty).orEmpty()
fun quoted(value: String) = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
android {
    namespace = "com.autodrive.app.core.network"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
        buildConfigField("String", "SUPABASE_URL", quoted(configurationValue("AUTODRIVE_SUPABASE_URL", "SUPABASE_URL")))
        buildConfigField("String", "SUPABASE_ANON_KEY", quoted(configurationValue("AUTODRIVE_SUPABASE_ANON_KEY", "SUPABASE_ANON_KEY")))
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { buildConfig = true }
}
dependencies {
    api(platform(libs.supabase.bom))
    api(libs.supabase.postgrest); api(libs.supabase.auth); api(libs.supabase.realtime)
    api(libs.supabase.storage); api(libs.supabase.functions)
    api(libs.ktor.client.core); api(libs.ktor.client.okhttp)
    implementation(libs.hilt.android); ksp(libs.hilt.compiler)
}
