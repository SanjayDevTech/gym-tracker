# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html
#
# Intentionally empty. Every third-party library used here (kotlinx-serialization 1.9,
# Room 2.8, AndroidX Lifecycle/Navigation, Compose, Material3, kotlinx-coroutines, DataStore)
# ships consumer R8/ProGuard rules inside its own AAR/JAR, which R8 picks up automatically.
# Add rules below only if a specific release-build runtime failure proves we need them.
