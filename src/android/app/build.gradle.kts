plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.harshkanjariya.wordwar"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.harshkanjariya.wordwar"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("../../../keys/wordwar.jks")
            storePassword = "wordwarspirits"
            keyAlias = "release"
            keyPassword = "wordwarspirits"
        }
    }

    buildTypes {
        flavorDimensions += "env"

        productFlavors {
            create("local") {
                dimension = "env"
                buildConfigField(
                    "String",
                    "BACKEND_URL",
                    "\"http://192.168.1.4:10000/api/\""
                )
            }
            create("staging") {
                dimension = "env"
                buildConfigField("String", "BACKEND_URL", "\"https://word-war-4.web.app/api/\"")
            }
            create("production") {
                dimension = "env"
                buildConfigField("String", "BACKEND_URL", "\"https://word-war-4.web.app/api/\"")
            }
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }

    packagingOptions {
        jniLibs {
            useLegacyPackaging = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material)

    // Import the Firebase BoM to manage library versions
    implementation(platform(libs.firebase.bom))

    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.functions)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore)

    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    implementation(libs.googleid)

    // Pluto core (only needed if using UI)
    debugImplementation(libs.pluto.core)
    releaseImplementation(libs.pluto.core.noop)

    // Network plugin
    debugImplementation(libs.pluto.network)
    releaseImplementation(libs.pluto.network.noop)
}