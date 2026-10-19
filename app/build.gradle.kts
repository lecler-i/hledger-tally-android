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

    signingConfigs {
        create("release") {
            // The keystore file is the secret; its password isn't.
            storeFile = file("release.p12")
            storePassword = "hledger-tally"
            keyAlias = "hledger-tally"
            keyPassword = "hledger-tally"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
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
