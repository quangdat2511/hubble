import java.util.Properties

fun normalizeBaseUrl(url: String): String {
    val trimmed = url.trim()
    return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
}

plugins {
    alias(libs.plugins.android.application)
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

    // Đọc local.properties (BASE_URL, GIPHY_API_KEY, …)
    val localProps = Properties()
    val androidLocalPropsFile = rootProject.file("local.properties")
    if (androidLocalPropsFile.exists()) androidLocalPropsFile.inputStream().use(localProps::load)
    val repoLocalPropsFile = rootProject.projectDir.parentFile.resolve("local.properties")
    if (repoLocalPropsFile.exists()) repoLocalPropsFile.inputStream().use(localProps::load)
    val localBaseUrl = normalizeBaseUrl(
        localProps.getProperty("BASE_URL_DEBUG", "http://10.0.2.2:8080/")
    )
    val giphyApiKey  = localProps.getProperty("GIPHY_API_KEY", "")

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL",     "\"$localBaseUrl\"")
            buildConfigField("String", "GIPHY_API_KEY", "\"$giphyApiKey\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL",     "\"https://hubble-production.up.railway.app/\"")
            buildConfigField("String", "GIPHY_API_KEY", "\"$giphyApiKey\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    val lifecycle_version = "2.8.7"
    implementation("com.github.yalantis:ucrop:2.2.11")
    implementation("androidx.lifecycle:lifecycle-viewmodel:${lifecycle_version}")
    implementation("androidx.lifecycle:lifecycle-livedata:${lifecycle_version}")
    implementation("androidx.lifecycle:lifecycle-runtime:${lifecycle_version}")

    implementation("com.hbb20:ccp:2.5.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.fragment:fragment:1.8.6")

    // Glide for GIF loading
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
