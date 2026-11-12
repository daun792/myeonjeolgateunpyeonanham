plugins { id("com.android.application") }
android {
    namespace = "com.example.pleosprobe"
    compileSdk = 37
    defaultConfig { applicationId = "com.example.pleosprobe"; minSdk = 28; targetSdk = 34 }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies { implementation("ai.pleos.playground:Vehicle:2.3.2") }
