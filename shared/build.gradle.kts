plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.serialization)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            //put your multiplatform dependencies here
            implementation("io.ktor:ktor-client-core:2.3.3")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.3")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.3")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
            implementation("com.russhwolf:multiplatform-settings:1.0.0")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
                implementation("io.ktor:ktor-client-okhttp:2.3.3")
                implementation("com.russhwolf:multiplatform-settings:1.0.0")
                //implementation("com.russhwolf:multiplatform-settings-android:1.1.1") // This is critical
        }
        iosMain.dependencies {
                implementation("io.ktor:ktor-client-darwin:2.3.3")
                //implementation("com.russhwolf:multiplatform-settings:1.0.0")
            implementation("com.russhwolf:multiplatform-settings:1.0.0")
            implementation("com.russhwolf:multiplatform-settings-coroutines:1.0.0")
        }
    }
}

android {
    namespace = "com.example.jans_chip"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
