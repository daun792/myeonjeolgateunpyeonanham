plugins {
    id("com.android.application")
}

providers.environmentVariable("PRIVACY_APP_BUILD_DIR").orNull
    ?.takeIf { it.isNotBlank() }
    ?.let { layout.buildDirectory.set(file(it)) }

// Release credentials are supplied by the local build environment, never committed.
val releaseStorePath = providers.environmentVariable("PRIVACY_RELEASE_STORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("PRIVACY_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("PRIVACY_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("PRIVACY_RELEASE_KEY_PASSWORD").orNull
val releaseCredentials = listOf(releaseStorePath, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
val hasReleaseCredentials = releaseCredentials.all { !it.isNullOrBlank() }

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

    signingConfigs {
        if (hasReleaseCredentials) {
            create("competitionRelease") {
                storeFile = file(requireNotNull(releaseStorePath))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (hasReleaseCredentials) signingConfig = signingConfigs.getByName("competitionRelease")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

// Prevent accidentally producing an unsigned artifact for submission.
val verifyReleaseSigning by tasks.registering {
    doLast {
        if (!hasReleaseCredentials) {
            throw GradleException("Set all PRIVACY_RELEASE_* signing environment variables before building a release.")
        }
    }
}
tasks.matching {
    it.name == "assembleRelease" || it.name == "bundleRelease"
            || it.name.startsWith("packageRelease") || it.name.startsWith("signRelease")
}.configureEach { dependsOn(verifyReleaseSigning) }
