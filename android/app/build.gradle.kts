plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.hubble"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.hubble"
        minSdk = 30
        targetSdk = 36
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

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    val lifecycle_version = "2.8.7"

    implementation(platform("com.google.firebase:firebase-bom:34.0.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    implementation("androidx.lifecycle:lifecycle-viewmodel:${lifecycle_version}")
    implementation("androidx.lifecycle:lifecycle-livedata:${lifecycle_version}")
    implementation("androidx.lifecycle:lifecycle-runtime:${lifecycle_version}")

    implementation("com.hbb20:ccp:2.5.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("com.google.android.gms:play-services-auth:21.0.0")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.fragment:fragment:1.8.6")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}