import java.util.Properties
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}
val localProperties = Properties().apply { val f=rootProject.file("local.properties"); if(f.exists()) f.inputStream().use(::load) }
fun config(name:String, legacy:String?=null):String = providers.gradleProperty(name).orNull ?: providers.environmentVariable(name).orNull ?: localProperties.getProperty(name) ?: legacy?.let(localProperties::getProperty).orEmpty()
fun quoted(value:String)="\"${value.replace("\\","\\\\").replace("\"","\\\"")}\""
android {
    namespace = "com.autodrive.app.core.platform"
    compileSdk = 35
    defaultConfig { minSdk=26; buildConfigField("String","ADMIN_WHATSAPP",quoted(config("AUTODRIVE_ADMIN_WHATSAPP","ADMIN_WHATSAPP"))) }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { buildConfig = true }
}
dependencies {
    implementation(project(":core:common"))
    api(project(":core:model")); api(project(":core:network")); api(project(":core:observability")); api(project(":core:session")); implementation(project(":core:designsystem"))
    implementation(libs.androidx.core.ktx); implementation(libs.coroutines.android)
    implementation(platform(libs.firebase.bom)); implementation(libs.firebase.messaging)
    implementation(platform(libs.supabase.bom)); implementation(libs.supabase.storage); implementation(libs.supabase.postgrest)
    implementation(libs.hilt.android); ksp(libs.hilt.compiler)
}
