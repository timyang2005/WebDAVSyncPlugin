import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
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

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.register<Copy>("renameApkToLnrp") {
    doLast {
        val apkDir = File(project.layout.buildDirectory.asFile.get(), "outputs/apk/debug")
        apkDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".apk")) {
                file.renameTo(File(file.parentFile, file.name.replace(".apk", ".apk.lnrp")))
            }
        }
    }
}

tasks.named("assembleDebug") {
    finalizedBy("renameApkToLnrp")
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

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.androidx.foundation.layout)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.runtime)

    // LNR Api
    implementation(libs.lightnovelreader.api)
}
