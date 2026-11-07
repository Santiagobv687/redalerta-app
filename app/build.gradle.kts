plugins {
    alias(libs.plugins.android.application)
    // Descomenta cuando configures Firebase (Sprint 3):
    // alias(libs.plugins.google-services)
}

android {
    namespace = "com.redalerta.client"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.redalerta.client"
        minSdk = 26                 // Android 8.0 (RNF-14)
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BASE_URL", "\"http://192.168.1.11:8080/api/v1/\"")
    }

    buildTypes {

        debug {
            buildConfigField("String", "BASE_URL", "\"http://192.168.1.11:8080/api/v1/\"")
        }

        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL", "\"https://api.redalerta.com/api/v1/\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // AndroidX base
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Lifecycle / ViewModel (MVVM)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.runtime)

    // Retrofit + OkHttp + Gson
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Glide (evidencias)
    implementation(libs.glide)

    // Mapa interactivo (OpenStreetMap)
    implementation(libs.osmdroid.android)

    // Efecto Shimmer (Skeleton UI)
    implementation(libs.shimmer)

    // Seguridad (JWT)
    implementation(libs.security.crypto)

    // Firebase (BOM + FCM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}