plugins {
    alias(libs.plugins.android.library)
//    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.hussein.mawaqit.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.batoul.prayerTimes)
    implementation(libs.gms.location)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.work.coroutines)
    implementation(libs.kotlinx.serialization.json)

}