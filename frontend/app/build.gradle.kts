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
// Nuova variabile per l'IP di Keycloak
val keycloakIp = localProperties.getProperty("KEYCLOAK_IP") ?: "10.0.2.2"

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

        // URL BACKEND (API Gateway tramite Ngrok)
        val finalUrl = if (backendIp.contains("ngrok")) {
            "https://$backendIp"
        } else if (backendIp.contains("http")) {
            backendIp
        } else {
            "http://$backendIp:8080"
        }

        // URL KEYCLOAK (Rete Locale)
        val keycloakUrl = if (keycloakIp.contains("http")) {
            keycloakIp
        } else {
            "http://$keycloakIp:8180"
        }

        buildConfigField("String", "BASE_URL", "\"$finalUrl\"")
        buildConfigField("String", "KEYCLOAK_BASE_URL", "\"$keycloakUrl\"")
        buildConfigField("String", "MAPS_API_KEY", "\"${localProperties.getProperty("MAPS_API_KEY") ?: ""}\"")

        // Schema URL per il reindirizzamento dopo il login con Keycloak
        manifestPlaceholders["appAuthRedirectScheme"] = "com.tripify.app"
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
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.org.json)
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

    // --- LIBRERIE AUTH & NETWORK ---
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // --- LIBRERIE PER I WEBSOCKETS (CHAT) ---
    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

    // --- LIBRERIA APPAUTH PER KEYCLOAK ---
    implementation("net.openid:appauth:0.11.1")

    // --- GENERAZIONE QR CODE (biglietto/check-in) ---
    // Solo il modulo "core": qui serve solo codificare una stringa in
    // un'immagine QR (biglietto in-app), non leggerla via fotocamera.
    implementation("com.google.zxing:core:3.5.3")
}