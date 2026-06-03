import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "eu.slickbot.avimar"

  defaultConfig {
    applicationId = namespace

    minSdk = 26
    targetSdk = 37
    compileSdk = 37

    versionCode = 1
    versionName = "0.1.0"

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
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
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

  testOptions {
    unitTests.isReturnDefaultValues = true
    unitTests.isIncludeAndroidResources = true
  }

  lint {
    // Timber is dragged in transitively by MapLibre, not used by us; ignore its
    // bundled rule nagging android.util.Log calls.
    disable += "LogNotTimber"
  }
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_21)
    optIn.addAll(
      "kotlinx.coroutines.ExperimentalCoroutinesApi",
      "kotlinx.coroutines.FlowPreview",
      "androidx.compose.material3.ExperimentalMaterial3Api",
      "kotlinx.serialization.ExperimentalSerializationApi",
    )
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.material3)
  implementation(libs.androidx.material.icons.extended)
  implementation(libs.maplibre.android.sdk)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.retrofit)
  implementation(libs.retrofit.kotlinx.serialization.converter)
  implementation(libs.okhttp)
  implementation(libs.okhttp.logging.interceptor)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.jsoup)
  implementation(libs.coil.compose)
  implementation(platform(libs.koin.bom))
  implementation(libs.koin.androidx.compose)

  debugImplementation(libs.androidx.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.okhttp.mockwebserver)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.test.ext.junit)

  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
