import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.jetbrains.compose.compiler)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "org.nsh07.wikireader"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.ekwoodrich.ewikireader"
        minSdk = 26
        targetSdk = 36
        versionCode = providers.gradleProperty("versionCode").orNull?.toIntOrNull() ?: 53
        versionName = providers.gradleProperty("versionName").orNull ?: "2.5.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        androidResources {
            generateLocaleConfig = true
        }
    }

    // CI supplies these through repository secrets; Android Studio can inject signing itself.
    val releaseKeystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
    val releaseStorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
    val releaseKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
    val releaseKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
    val releaseSigningValues = listOf(releaseKeystorePath, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
    require(releaseSigningValues.all { it.isNullOrBlank() } || releaseSigningValues.all { !it.isNullOrBlank() }) {
        "Release signing requires ANDROID_KEYSTORE_PATH, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS and ANDROID_KEY_PASSWORD together."
    }
    val hasReleaseSigning = releaseSigningValues.all { !it.isNullOrBlank() }
    if (hasReleaseSigning) {
        signingConfigs.create("release") {
            storeFile = file(releaseKeystorePath!!)
            storePassword = releaseStorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-eink-dev"
        }
        release {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

tasks.withType(Test::class) {
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        showStandardStreams = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.adaptive)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)

    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.coil3.coil.gif)
    implementation(libs.coil3.coil.svg)
    implementation(libs.coil3.compose)
    implementation(libs.coil3.network.okhttp)
    implementation(libs.ehsannarmani.compose.charts)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.material.kolor)
    implementation(libs.okhttp)
    implementation(libs.retrofit2.converter.scalars)
    implementation(libs.retrofit2.kotlinx.serialization.converter)
    implementation(libs.retrofit2.retrofit)
    implementation(libs.latex2unicode.x.x3)
    implementation(libs.androidx.profileinstaller)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    "baselineProfile"(project(":baselineprofile"))
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
