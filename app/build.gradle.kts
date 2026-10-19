plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.ghjkl.tally"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.ghjkl.tally"
        minSdk = 26
        versionCode = 4
        versionName = "0.2.0"
    }

    buildTypes {
        release {
            // CI signs with the release keystore; local builds without one use the debug key.
            signingConfig = System.getenv("TALLY_KEYSTORE")?.let { keystore ->
                signingConfigs.create("release") {
                    storeFile = file(keystore)
                    storePassword = System.getenv("TALLY_KEYSTORE_PASSWORD")
                    keyAlias = "tally"
                    keyPassword = System.getenv("TALLY_KEYSTORE_PASSWORD")
                }
            } ?: signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.commons.text)
}
