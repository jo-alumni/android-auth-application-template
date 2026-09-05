import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

plugins {
    id("authapplication.android.library")
    id("authapplication.hilt")
    alias(libs.plugins.protobuf)
}

android {
    namespace = "com.example.authapplication.data"
}

// protobuf-gradle-pluginが生成するjava/kotlinソースを、AGP built-in KotlinのKSPが
// 参照できる kotlin.sourceSets DSL 経由で明示的に追加する
// (gradle.properties の android.disallowKotlinSourceSets=false 参照)。
// AGPのbuilt-in Kotlinはvariant用のKotlinSourceSetを遅延生成するため afterEvaluate で登録する。
afterEvaluate {
    kotlin.sourceSets.getByName("debug") {
        kotlin.srcDir(tasks.named("generateDebugProto"))
    }
    kotlin.sourceSets.getByName("release") {
        kotlin.srcDir(tasks.named("generateReleaseProto"))
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.28.2"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
                create("kotlin") {
                    option("lite")
                }
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
    implementation(projects.domain)
    implementation(libs.findLibrary("androidx-core-ktx").get())
    implementation(libs.findLibrary("androidx-datastore-core").get())
    implementation(libs.findLibrary("kotlinx-coroutines-core").get())
    implementation(libs.findLibrary("tink-android").get())
    implementation(libs.findLibrary("protobuf-kotlin-lite").get())
    implementation(libs.findLibrary("protobuf-javalite").get())

    testImplementation(libs.findLibrary("junit").get())
    testImplementation(libs.findLibrary("kotlinx-coroutines-core").get())
}
