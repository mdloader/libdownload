import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.include

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "video.dldownloader"
    compileSdk = 34

    defaultConfig {
        applicationId = "video.dldownloader"
        minSdk = 24
        targetSdk = 34
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
    buildFeatures{viewBinding = true}
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":lib"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.ads.lite)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation  ("androidx.lifecycle:lifecycle-process:2.6.1" )
    // RxJava dependency
    implementation("io.reactivex.rxjava3:rxjava:3.1.5")

    // RxAndroid dependency
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")

    implementation("io.github.junkfood02.youtubedl-android:library:0.17.2")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.17.2")
    implementation("io.github.junkfood02.youtubedl-android:aria2c:0.17.2")

    implementation ("com.airbnb.android:lottie:6.1.0")

    // Media3 dependencies

    implementation ("androidx.media3:media3-exoplayer:1.2.0")
    implementation ("androidx.media3:media3-ui:1.2.0")
    implementation ("androidx.media3:media3-common:1.2.0")

    // Glide for thumbnails
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")


    implementation("com.google.android.gms:play-services-ads:23.6.0")
    // Fallback for older devices
    implementation ("com.google.android.gms:play-services-base:18.2.0")

    implementation(libs.jackson.databind)
    implementation(libs.jackson.annotations)
    implementation(libs.commons.io)
}