// Top-level build file — plugins are declared here with `apply false`
// and applied in the module build files.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    // Applied in :app once google-services.json exists (Phase 1 / 4b):
    alias(libs.plugins.google.services) apply false
}
