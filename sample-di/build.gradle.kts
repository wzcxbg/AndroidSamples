plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.sliver.sampledi"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sliver.sampledi"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")

    // A fast dependency injector for Android and Java.
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")

    // AndroidX Hilt Extension Annotations
    implementation("androidx.hilt:hilt-common:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Android Lifecycle WorkManager Hilt Extension
    implementation("androidx.hilt:hilt-work:1.2.0")

    // AndroidX Hilt Extension Compiler
    implementation("androidx.hilt:hilt-navigation:1.2.0")

    // Android Navigation Fragment Hilt Extension
    implementation("androidx.hilt:hilt-navigation-fragment:1.2.0")

    // Navigation Compose Hilt Integration
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
}