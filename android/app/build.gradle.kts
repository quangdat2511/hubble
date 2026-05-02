import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

fun String.toBuildConfigString(): String {
    return "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

android {
    namespace = "com.example.hubble"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.hubble"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Đọc local.properties (BASE_URL, GIPHY_API_KEY, …)
    val localProps = Properties()

    fun loadPropertiesIfExists(path: String) {
        val file = rootProject.file(path)
        if (file.exists()) {
            file.inputStream().use(localProps::load)
        }
    }

    fun propertyOrEnv(name: String, defaultValue: String = ""): String {
        val propertyValue = localProps.getProperty(name)?.trim().orEmpty()
        if (propertyValue.isNotEmpty()) {
            return propertyValue
        }

        val envValue = System.getenv(name)?.trim().orEmpty()
        if (envValue.isNotEmpty()) {
            return envValue
        }

        return defaultValue
    }

    // Support both android/local.properties and the repo-root local.properties.
    // The android-local file wins if both define the same key.
    loadPropertiesIfExists("../local.properties")
    loadPropertiesIfExists("local.properties")

    val releaseBaseUrl = "https://hubble-production.up.railway.app/"
    val debugBaseUrlOverride = propertyOrEnv("BASE_URL_DEBUG")
    val devBackendScheme = propertyOrEnv("DEV_BACKEND_SCHEME", "http")
    val devBackendHost = propertyOrEnv("DEV_BACKEND_HOST")
    val devBackendPort = propertyOrEnv("DEV_BACKEND_PORT", "8080")
    val giphyApiKey = propertyOrEnv("GIPHY_API_KEY")

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", releaseBaseUrl.toBuildConfigString())
            buildConfigField("String", "DEBUG_BASE_URL_OVERRIDE", debugBaseUrlOverride.toBuildConfigString())
            buildConfigField("String", "DEV_BACKEND_SCHEME", devBackendScheme.toBuildConfigString())
            buildConfigField("String", "DEV_BACKEND_HOST", devBackendHost.toBuildConfigString())
            buildConfigField("String", "DEV_BACKEND_PORT", devBackendPort.toBuildConfigString())
            buildConfigField("String", "GIPHY_API_KEY", giphyApiKey.toBuildConfigString())
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL", releaseBaseUrl.toBuildConfigString())
            buildConfigField("String", "DEBUG_BASE_URL_OVERRIDE", "\"\"")
            buildConfigField("String", "DEV_BACKEND_SCHEME", "\"https\"")
            buildConfigField("String", "DEV_BACKEND_HOST", "\"\"")
            buildConfigField("String", "DEV_BACKEND_PORT", "\"\"")
            buildConfigField("String", "GIPHY_API_KEY", giphyApiKey.toBuildConfigString())
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

    lint {
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    val lifecycle_version = "2.8.7"

    implementation("androidx.lifecycle:lifecycle-viewmodel:${lifecycle_version}")
    implementation("androidx.lifecycle:lifecycle-livedata:${lifecycle_version}")
    implementation("androidx.lifecycle:lifecycle-runtime:${lifecycle_version}")
    implementation("androidx.lifecycle:lifecycle-process:${lifecycle_version}")

    implementation("com.hbb20:ccp:2.5.0")

    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation ("com.google.firebase:firebase-messaging")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

    implementation("com.google.android.gms:play-services-auth:21.0.0")
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.github.yalantis:ucrop:2.2.11")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.fragment:fragment:1.8.6")
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // WebRTC for voice/video calls
    implementation("io.getstream:stream-webrtc-android:1.1.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
