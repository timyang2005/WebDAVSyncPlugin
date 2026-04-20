import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.dobao.webdavsync"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dobao.webdavsync"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp3.okhttp)
    implementation(libs.okhttp3.logging.interceptor)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation.layout)
    implementation(libs.lightnovelreader.api)
}

// Rename debug APK to .lnrp
afterEvaluate {
    tasks.named("assembleDebug") {
        doLast {
            val apkDir = project.layout.buildDirectory.dir("outputs/apk/debug").get().asFile
            val apkFile = apkDir.listFiles()?.find { it.name.endsWith(".apk") }
            if (apkFile != null) {
                val renamed = File(apkDir, apkFile.name.replace(".apk", ".apk.lnrp"))
                apkFile.renameTo(renamed)
                println("Renamed: ${renamed.name}")
            }
        }
    }
}
