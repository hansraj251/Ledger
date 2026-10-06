plugins {
    id("com.android.legacy-kapt")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.android.application")
}

android {
    namespace = "com.ledger.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ledger.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

dependencies {
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    kapt("androidx.room:room-compiler:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")
    implementation("androidx.room:room-runtime:2.8.5")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.room:room-testing:2.8.5")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")


    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
