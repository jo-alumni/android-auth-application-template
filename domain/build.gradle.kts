import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

plugins {
    id("authapplication.jvm.library")
    id("java-test-fixtures")
}

// NOTE: TYPESAFE_PROJECT_ACCESSORS(projects.xxx)有効時、build-logic(included build)由来の
// プラグインIDを plugins{} で適用したスクリプトでは `libs.xxx` の型安全アクセサが解決できなくなる
// (Gradleの既知の制限)。そのため、同名の`libs`をローカルvalとして再定義し、
// `libs.findLibrary("...")` 経由でカタログを参照する。
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(libs.findLibrary("kotlinx-coroutines-core").get())
    implementation(libs.findLibrary("javax-inject").get())

    // FakeAuthRepository/MainDispatcherRuleなど、:app・:app:feature:login等の
    // 複数モジュールから共通で使うテスト用フェイクを testFixtures として公開する。
    testFixturesApi(libs.findLibrary("junit").get())
    testFixturesApi(libs.findLibrary("kotlinx-coroutines-core").get())
    testFixturesApi(libs.findLibrary("kotlinx-coroutines-test").get())

    testImplementation(libs.findLibrary("junit").get())
    testImplementation(libs.findLibrary("kotlinx-coroutines-test").get())
    testImplementation(libs.findLibrary("turbine").get())
}
