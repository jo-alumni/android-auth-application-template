import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

plugins {
    id("authapplication.android.library")
    id("authapplication.android.compose")
    id("authapplication.kotlin.serialization")
}

android {
    namespace = "com.example.authapplication.core"
}

// NOTE: TYPESAFE_PROJECT_ACCESSORS(projects.xxx)有効時、build-logic(included build)由来の
// プラグインIDを plugins{} で適用したスクリプトでは `libs.xxx` の型安全アクセサが解決できなくなる
// (Gradleの既知の制限)。そのため、同名の`libs`をローカルvalとして再定義し、
// `libs.findLibrary("...")` 経由でカタログを参照する。
val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(libs.findLibrary("androidx-compose-material-icons-core").get())
}
