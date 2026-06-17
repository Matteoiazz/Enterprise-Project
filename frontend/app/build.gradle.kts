import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
val backendIp = localProperties.getProperty("BACKEND_IP") ?: "10.0.2.2"

android {
    namespace = "com.tripify.tripify_android"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.tripify.tripify_android"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // CONTROLLO INTELLIGENTE AGGIORNATO
        val finalUrl = if (backendIp.contains("ngrok")) {
            "https://$backendIp" // Se è Ngrok, usa HTTPS e NIENTE porta
        } else if (backendIp.contains("http")) {
            backendIp // Se ha già http, lo lascia così com'è
        } else {
            "http://$backendIp:8080" // Se è un IP classico (es. 10.0.2.2), mette HTTP e porta 8080
        }

        buildConfigField("String", "BASE_URL", "\"$finalUrl\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    val composeBom = platform("androidx.compose:compose-bom:2024.04.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("io.coil-kt:coil-compose:2.6.0")

    // --- LIBRERIE PER DARIO (AUTH & NETWORK) ---

    // Retrofit per le chiamate API al Gateway
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // OkHttp per l'Interceptor (inietta il Token JWT in automatico)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // DataStore (La cassaforte per salvare il JWT in modo persistente)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Navigation Compose (per spostarsi tra le schermate)
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Lifecycle e Coroutines (Per chiamate asincrone senza bloccare la UI)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
}