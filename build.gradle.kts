// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.googleServices) apply false
    alias(libs.plugins.firebaseCrashlytics) apply false
    alias(libs.plugins.kotlinCompose) apply false
    alias(libs.plugins.kover)
}

dependencies {
    // Merged coverage for modest verify gate (see docs/TESTING.md).
    kover(projects.core.data)
    kover(projects.core.domain)
    kover(projects.feature.home)
}

kover {
    // Root has no Android sources; map each dependency's debug into a shared report variant.
    currentProject {
        createVariant("merged") {
            add("debug", optional = true)
        }
    }
    reports {
        filters {
            excludes {
                // Generated / Android glue — keep the gate focused on app logic.
                androidGeneratedClasses()
                classes(
                    "*.BuildConfig",
                    "*.R",
                    "*.R$*",
                    "*.databinding.*",
                )
                annotatedBy(
                    "androidx.compose.runtime.Composable",
                    "androidx.compose.ui.tooling.preview.Preview",
                )
            }
        }
        variant("merged") {
            verify {
                rule("Modest line coverage (:core:data, :core:domain, :feature:home)") {
                    minBound(8)
                }
            }
        }
    }
}
