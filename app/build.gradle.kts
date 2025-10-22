    plugins {
        alias(libs.plugins.android.application)
        alias(libs.plugins.kotlin.android)
        alias(libs.plugins.kotlin.compose) // ✅ required for Kotlin 2.0+
        id("com.google.gms.google-services")
    }

    android {
        namespace = "com.example.signbuddy"
        compileSdk = 36

        defaultConfig {
            applicationId = "com.example.signbuddy"
            minSdk = 24
            targetSdk = 36
            versionCode = 1
            versionName = "1.0"
        }

        buildFeatures {
            compose = true
        }

        kotlinOptions {
            jvmTarget = "17"
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }

    dependencies {
        // Compose BOM
        implementation(platform(libs.androidx.compose.bom))

        // Core Android
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.activity.compose)

        // Compose UI
        implementation(libs.androidx.compose.ui)
        implementation(libs.androidx.compose.ui.graphics)
        implementation(libs.androidx.compose.ui.tooling.preview)
        implementation(libs.androidx.compose.material3)
        implementation(libs.androidx.compose.runtime.saveable)

        // Compose Navigation & icons
        implementation("androidx.navigation:navigation-compose:2.8.3")
        implementation("androidx.compose.material:material-icons-extended")
        // Accompanist Navigation Animation for screen transitions
        implementation("com.google.accompanist:accompanist-navigation-animation:0.36.0")

        // CameraX
        implementation("androidx.camera:camera-camera2:1.5.0")
        implementation("androidx.camera:camera-lifecycle:1.5.0")
        implementation("androidx.camera:camera-view:1.5.0")

        // ✅ TensorFlow Lite (core + GPU + support + task vision)
        implementation("org.tensorflow:tensorflow-lite:2.14.0")
        implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
        implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
        implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.4")

        implementation("com.google.mediapipe:tasks-vision:0.10.14")
        implementation(libs.androidx.compose.foundation)

        // Firebase
        implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
        implementation("com.google.firebase:firebase-auth-ktx")
        implementation("com.google.firebase:firebase-firestore-ktx")
        implementation("com.google.firebase:firebase-analytics-ktx")

        // Debug
        debugImplementation(libs.androidx.compose.ui.tooling)
        debugImplementation(libs.androidx.compose.ui.test.manifest)

        // Testing
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(platform(libs.androidx.compose.bom))
        androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    }

