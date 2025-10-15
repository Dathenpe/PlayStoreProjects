// build.gradle.kts (app module)
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.f9ld3.Zion"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.f9ld3.Zion"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "2.0"

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
    // Existing dependencies
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // ----------------------------------------------------------------------
    // Firebase Dependencies (STANDARD JAVA VERSIONS - NO KTX)
    // ----------------------------------------------------------------------

    // Firebase BOM (Bill of Materials) for version management
    implementation(platform(libs.firebase.bom))

    // Firebase Core Services (Auth, Firestore, Storage, Messaging) - Use standard Java libs
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)

    // ExoPlayer (Jetpack Media3)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // Glide (Image Loading)
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // CircleImageView
    implementation(libs.circleimageview)

    // AndroidX Activity (for registerForActivityResult, etc.)
    implementation(libs.activity)

    // Preferences for SettingsFragment - Using non-KTX version for Java project
    implementation(libs.preference)

    // ----------------------------------------------------------------------

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}