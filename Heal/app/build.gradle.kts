plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.f9ld3.heal"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.f9ld3.heal"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "2.78"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // These fields are correctly configured to read from gradle.properties
        buildConfigField("String", "GEMINI_API_KEY",    "\"${project.properties["GEMINI_API_KEY"] as String? ?: System.getenv("GEMINI_API_KEY") ?: ""}\"")
        buildConfigField("String", "JIRA_BASE_URL",     "\"${project.properties["JIRA_BASE_URL"] as String? ?: System.getenv("JIRA_BASE_URL") ?: ""}\"")
        buildConfigField("String", "JIRA_API_USERNAME", "\"${project.properties["JIRA_API_USERNAME"] as String? ?: System.getenv("JIRA_API_USERNAME") ?: ""}\"")
        buildConfigField("String", "JIRA_API_TOKEN",    "\"${project.properties["JIRA_API_TOKEN"] as String? ?: System.getenv("JIRA_API_TOKEN") ?: ""}\"")
        buildConfigField("String", "JIRA_PROJECT_KEY",  "\"${project.properties["JIRA_PROJECT_KEY"] as String? ?: System.getenv("JIRA_PROJECT_KEY") ?: ""}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    tasks.withType(JavaCompile::class.java) {
        options.compilerArgs.add("-Xlint:deprecation")
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation("net.objecthunter:exp4j:0.4.8")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.mp.android.chart)
    implementation(libs.gson)
    implementation(libs.flexbox)
    implementation(libs.viewpager2)
    implementation(libs.circleimageview)
    implementation(libs.glide)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp3.logging.interceptor)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.lifecycle.viewmodel.ktx)
    annotationProcessor(libs.glide.compiler)
    implementation(libs.work.runtime)
    implementation(libs.media3.common)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.preference)
    implementation(libs.legacy.support.v4)
    implementation(libs.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}