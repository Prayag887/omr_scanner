import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 35

    defaultConfig {
//        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 35
//        versionCode = 1
//        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++11"
                arguments += "-DANDROID_STL=c++_shared"
            }
        }
        ndk {
            abiFilters += setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }

        buildFeatures {
            aidl = true
            buildConfig = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // Add OpenCV libraries to the jniLibs directory
    sourceSets {
        named("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}


// get the github token from secrets properties file
val secretsPropertiesFile = rootProject.file("secrets.properties")
val secrets = Properties()

val githubToken: String by lazy {
    val propsFile = rootProject.file("secrets.properties")
    val props = Properties()
    props.load(FileInputStream(propsFile))
    props.getProperty("GITHUB_TOKEN") ?: throw GradleException("GITHUB_TOKEN not found in secrets.properties")
}

    //publish the aar file to the github package registry as maven package
publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.prayag"
            artifactId = "omr-scanner"
            version = "0.1.4"

            afterEvaluate {
                from(components["release"])
            }
        }
    }

    repositories{
        maven {
            name = "omr_package"
            url = uri("https://maven.pkg.github.com/Prayag887/omr_scanner")
            credentials {
                username = "Prayag887"
                password = githubToken
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(project(":OpenCV-sdk"))

    // Latest ML Kit Document Scanner dependency
//    implementation ("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")

 // For ML Kit base functionality
    implementation ("com.google.mlkit:vision-common:17.3.0")

    // CameraX Core Library
    implementation ("androidx.camera:camera-core:1.4.2")

    // CameraX Camera2 Implementation
    implementation ("androidx.camera:camera-camera2:1.4.2")

    // CameraX Lifecycle
    implementation ("androidx.camera:camera-lifecycle:1.4.2")

    // CameraX View for Preview
    implementation ("androidx.camera:camera-view:1.4.2")

    // Optional: For image analysis
    implementation ("androidx.camera:camera-extensions:1.4.2")


    implementation(libs.document.scanner)

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))

    //shimmer effect for the ui
    implementation ("com.facebook.shimmer:shimmer:0.5.0")

    implementation ("com.google.android.material:material:1.12.0")

    implementation("io.insert-koin:koin-android:3.5.0")

    implementation("io.insert-koin:koin-core:3.5.0")

    testImplementation("io.insert-koin:koin-test-junit5:3.5.0")

}
