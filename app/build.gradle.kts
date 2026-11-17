import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

fun getGitVersionCode(): Int {
    val propCode = (project.findProperty("versionCode") as? String)?.toIntOrNull()
        ?: System.getenv("VERSION_CODE")?.toIntOrNull()
    if (propCode != null) return propCode

    return try {
        val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        val count = output.toIntOrNull() ?: 1
        count.coerceAtLeast(1)
    } catch (e: Exception) {
        1
    }
}

fun getGitVersionName(): String {
    val propName = (project.findProperty("versionName") as? String)
        ?: System.getenv("VERSION_NAME")
    if (!propName.isNullOrBlank()) return propName

    return try {
        val process = ProcessBuilder("git", "describe", "--tags", "--always")
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        if (output.isNotEmpty()) {
            output.removePrefix("v")
        } else {
            "1.0.0"
        }
    } catch (e: Exception) {
        "1.0.0"
    }
}

val computedVersionCode = getGitVersionCode()
val computedVersionName = getGitVersionName()

android {
    namespace = "com.vision202020.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vision202020.app"
        minSdk = 26
        targetSdk = 34
        versionCode = computedVersionCode
        versionName = computedVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_FILE") ?: project.findProperty("KEYSTORE_FILE") as? String
            val keystorePass = System.getenv("KEYSTORE_PASSWORD") ?: project.findProperty("KEYSTORE_PASSWORD") as? String
            val aliasName = System.getenv("KEY_ALIAS") ?: project.findProperty("KEY_ALIAS") as? String
            val keyPass = System.getenv("KEY_PASSWORD") ?: project.findProperty("KEY_PASSWORD") as? String

            if (keystorePath != null && File(keystorePath).exists()) {
                storeFile = File(keystorePath)
                storePassword = keystorePass
                keyAlias = aliasName
                keyPassword = keyPass
            } else {
                // Fall back to debug keystore if custom release keystore not provided
                val debugKeystore = File("${System.getProperty("user.home")}/.android/debug.keystore")
                if (debugKeystore.exists()) {
                    storeFile = debugKeystore
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                }
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
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
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

tasks.register("printVersion") {
    doLast {
        println("versionCode=$computedVersionCode")
        println("versionName=$computedVersionName")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)

    debugImplementation(libs.androidx.ui.tooling)
}
