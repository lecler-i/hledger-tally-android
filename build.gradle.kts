// CodeQL's autobuild only detects an Android project from the root build file.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
