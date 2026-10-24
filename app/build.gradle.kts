import java.util.Properties

plugins { id("com.android.application") }
val releaseSigning = listOf("SIGNING_STORE_FILE", "SIGNING_STORE_PASSWORD", "SIGNING_KEY_ALIAS", "SIGNING_KEY_PASSWORD")
    .all { !System.getenv(it).isNullOrBlank() }
// Owner-specific baselines are optional and never checked into the public repository.
val baseline = Properties()
val baselineFile = rootProject.file("local-baseline.properties")
if (baselineFile.exists()) baselineFile.inputStream().use { baseline.load(it) }
fun baselineLiteral(key: String): String {
    val value = baseline.getProperty(key, "")
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""
}
android {
    namespace = "com.elitedarkkaiser.nubialy"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.elitedarkkaiser.nubialy"
        minSdk = 35
        targetSdk = 35
        buildConfigField("String", "BASELINE_FINGERPRINT", baselineLiteral("fingerprint"))
        buildConfigField("String", "BASELINE_SYSTEM_UI_SHA256", baselineLiteral("systemUiSha256"))
        versionCode = 107
        versionName = "1.1.1-activation-test"
    }
    signingConfigs {
        if (releaseSigning) create("persistent") {
            storeFile = file(System.getenv("SIGNING_STORE_FILE"))
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
        }
    }
    buildTypes {
        debug {
            if (releaseSigning) signingConfig = signingConfigs.getByName("persistent")
        }
        release {
            isMinifyEnabled = false
            if (releaseSigning) signingConfig = signingConfigs.getByName("persistent")
        }
    }
    buildFeatures { buildConfig = true }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.testLogging.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            it.jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED",
                "--add-opens=java.base/java.net=ALL-UNNAMED",
                "--add-opens=java.base/java.security=ALL-UNNAMED",
                "--add-opens=java.base/java.text=ALL-UNNAMED",
                "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
                "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    compileOnly("de.robv.android.xposed:api:82")
    testImplementation("junit:junit:4.13.2")
    testImplementation("de.robv.android.xposed:api:82")
    testImplementation("org.robolectric:robolectric:4.17")
}
