plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "com.example.authapplication.data"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// protobuf-gradle-pluginが生成するjava/kotlinソースを、AGP built-in KotlinのKSPが
// 参照できる kotlin.sourceSets DSL 経由で明示的に追加する
// (gradle.properties の android.disallowKotlinSourceSets=false 参照)。
// AGPのbuilt-in Kotlinはvariant用のKotlinSourceSetを遅延生成するため afterEvaluate で登録する。
afterEvaluate {
    // srcDirにTaskProviderを渡すことで、Gradleが自動的にタスク依存関係を解決する。
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

dependencies {
    implementation(projects.domain)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.hilt.android)
    implementation(libs.tink.android)
    implementation(libs.protobuf.kotlin.lite)
    implementation(libs.protobuf.javalite)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.core)
}
