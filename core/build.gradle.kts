plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.hussein.mawaqit.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.batoul.prayerTimes)
    implementation(libs.gms.location)
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.work.coroutines)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
}
