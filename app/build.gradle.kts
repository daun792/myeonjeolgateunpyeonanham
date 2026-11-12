plugins {
    id("com.android.application")
}

providers.environmentVariable("PRIVACY_APP_BUILD_DIR").orNull
    ?.takeIf { it.isNotBlank() }
    ?.let { layout.buildDirectory.set(file(it)) }

android {
    namespace = "com.example.vehicleprivacy"
    compileSdk = 37
    useLibrary("android.car")

    defaultConfig {
        applicationId = "com.example.vehicleprivacy"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
