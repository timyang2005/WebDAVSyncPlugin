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
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp3.okhttp)
    implementation(libs.okhttp3.logging.interceptor)
    implementation(libs.androidx.core.ktx)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation.layout)

    implementation(libs.lightnovelreader.api)

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}

afterEvaluate {
    tasks.register<Copy>("renameApk") {
        description = "Rename debug APK to .apk.lnrp"
        doFirst {
            def apkDir = new File(project.buildDir, "outputs/apk/debug")
            if (apkDir.exists()) {
                def apkFiles = apkDir.listFiles { f -> f.name.endsWith(".apk") }
                if (apkFiles && apkFiles.length > 0) {
                    def apk = apkFiles[0]
                    def newName = apk.name.replace(".apk", ".apk.lnrp")
                    def dest = new File(apkDir, newName)
                    if (dest.exists()) dest.delete()
                    project.copy {
                        from apk
                        into apkDir
                        rename { newName }
                    }
                    println "Renamed:  -> "
                }
            }
        }
    }

    tasks.matching { it.name == "assembleDebug" }.configureEach {
        finalizedBy("renameApk")
    }
}