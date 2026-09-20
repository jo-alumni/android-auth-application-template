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
    implementation(projects.app.feature.notification)
    implementation(projects.domain)
    implementation(projects.data)

    implementation(libs.findLibrary("androidx-core-ktx").get())
    // TopLevelDestination（ボトムバー/レール/ドロワーの項目）のアイコンに使う。
    implementation(libs.findLibrary("androidx-compose-material-icons-core").get())
    // 認証状態が確定するまでスプラッシュを維持するために使う（docs/auth-navigation.md 参照）。
    implementation(libs.findLibrary("androidx-core-splashscreen").get())
    // 画面幅(WindowSizeClass)に応じてナビゲーションUIを出し分けるために使う。
    implementation(libs.findLibrary("androidx-compose-material3-adaptive").get())
    implementation(libs.findLibrary("androidx-lifecycle-runtime-ktx").get())

    testImplementation(libs.findLibrary("kotlinx-coroutines-test").get())
    testImplementation(libs.findLibrary("turbine").get())
    testImplementation(testFixtures(projects.domain))
    // AppStateTestが実際のNavController(TestNavHostController)を組み立てるために使う。
    // Robolectric本体はauthapplication.android.compose Convention Pluginが追加している。
    testImplementation(libs.findLibrary("androidx-navigation-testing").get())
    androidTestImplementation(platform(libs.findLibrary("androidx-compose-bom").get()))
    androidTestImplementation(libs.findLibrary("androidx-compose-ui-test-junit4").get())
    androidTestImplementation(libs.findLibrary("androidx-espresso-core").get())
    androidTestImplementation(libs.findLibrary("androidx-junit").get())
    androidTestImplementation(libs.findLibrary("hilt-android-testing").get())
    androidTestImplementation(libs.findLibrary("androidx-navigation-testing").get())
    // @TestInstallIn でリポジトリ実装を差し替えるためのFake群。
    androidTestImplementation(testFixtures(projects.domain))
    kspAndroidTest(libs.findLibrary("hilt-compiler").get())
}
