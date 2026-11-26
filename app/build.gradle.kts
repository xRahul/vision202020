import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.cyclonedx.bom)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.kover)
}

val versionProps = Properties().apply {
    val file = rootProject.file("version.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun getGitVersionCode(): Int {
    (project.findProperty("versionCode") as? String)?.toIntOrNull()?.let { return it }
    System.getenv("VERSION_CODE")?.toIntOrNull()?.let { return it }
    versionProps.getProperty("versionCode")?.toIntOrNull()?.let { return it }

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
    (project.findProperty("versionName") as? String)?.takeIf { it.isNotBlank() }?.let { return it }
    System.getenv("VERSION_NAME")?.takeIf { it.isNotBlank() }?.let { return it }
    versionProps.getProperty("versionName")?.takeIf { it.isNotBlank() }?.let { return it }

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
    namespace = "in.rahulja.vision202020"
    compileSdk = 36

    defaultConfig {
        applicationId = "in.rahulja.vision202020"
        minSdk = 26
        targetSdk = 36
        versionCode = computedVersionCode
        versionName = computedVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Only register a release signing config when a real keystore is present.
        // Without one the APK stays unsigned instead of being silently debug-signed.
        val keystorePath = System.getenv("KEYSTORE_FILE")
            ?: project.findProperty("KEYSTORE_FILE") as? String
        val keystoreFile = keystorePath?.let { File(it) }?.takeIf { it.exists() }
        if (keystoreFile != null) {
            create("release") {
                storeFile = keystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                    ?: project.findProperty("KEYSTORE_PASSWORD") as? String
                keyAlias = System.getenv("KEY_ALIAS")
                    ?: project.findProperty("KEY_ALIAS") as? String
                keyPassword = System.getenv("KEY_PASSWORD")
                    ?: project.findProperty("KEY_PASSWORD") as? String
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            // Keep the APK independent of the recipe commit so F-Droid can reproduce it.
            vcsInfo.include = false
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

    lint {
        sarifReport = true
        warningsAsErrors = true
        abortOnError = true
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = false
            isIncludeAndroidResources = true
            // Robolectric fetches its SDK jar from repo1.maven.org at test runtime,
            // outside Gradle's dependency cache; point it at the mirror to avoid 429s.
            all {
                it.systemProperty(
                    "robolectric.dependency.repo.url",
                    "https://maven-central.storage-download.googleapis.com/maven2"
                )
            }
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

kover {
    reports {
        variant("debug") {
            xml {
                onCheck = false
            }
            verify {
                rule {
                    minBound(50)
                }
            }
        }
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
    implementation(libs.aboutlibraries.compose.m3)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.ui.test.junit4)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.uiautomator)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
