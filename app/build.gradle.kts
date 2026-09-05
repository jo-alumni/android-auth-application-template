import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

plugins {
    id("authapplication.android.application")
    id("authapplication.android.compose")
    id("authapplication.android.feature")
}

android {
    namespace = "com.example.authapplication"

    defaultConfig {
        applicationId = "com.example.authapplication"
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.example.authapplication.HiltTestRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
}

// NOTE: TYPESAFE_PROJECT_ACCESSORS(projects.xxx)有効時、build-logic(included build)由来の
// プラグインIDを plugins{} で適用したスクリプトでは `libs.xxx` の型安全アクセサが解決できなくなる
// (Gradleの既知の制限)。そのため、同名の`libs`をローカルvalとして再定義し、
// `libs.findLibrary("...")` 経由でカタログを参照する。
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(projects.app.core)
    implementation(projects.app.feature.login)
    implementation(projects.app.feature.home)
    implementation(projects.app.feature.search)
    implementation(projects.app.feature.favorite)
    implementation(projects.app.feature.detail)
    implementation(projects.domain)
    implementation(projects.data)

    implementation(libs.findLibrary("androidx-core-ktx").get())
    implementation(libs.findLibrary("androidx-lifecycle-runtime-ktx").get())

    testImplementation(libs.findLibrary("junit").get())
    testImplementation(libs.findLibrary("kotlinx-coroutines-test").get())
    testImplementation(libs.findLibrary("turbine").get())
    testImplementation(testFixtures(projects.domain))
    androidTestImplementation(platform(libs.findLibrary("androidx-compose-bom").get()))
    androidTestImplementation(libs.findLibrary("androidx-compose-ui-test-junit4").get())
    androidTestImplementation(libs.findLibrary("androidx-espresso-core").get())
    androidTestImplementation(libs.findLibrary("androidx-junit").get())
    androidTestImplementation(libs.findLibrary("hilt-android-testing").get())
    androidTestImplementation(libs.findLibrary("androidx-navigation-testing").get())
    kspAndroidTest(libs.findLibrary("hilt-compiler").get())
    debugImplementation(libs.findLibrary("androidx-compose-ui-test-manifest").get())
    debugImplementation(libs.findLibrary("androidx-compose-ui-tooling").get())
}
