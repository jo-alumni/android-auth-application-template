plugins {
    `kotlin-dsl`
}

group = "com.example.authapplication.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "authapplication.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "authapplication.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "authapplication.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("kotlinSerialization") {
            id = "authapplication.kotlin.serialization"
            implementationClass = "KotlinSerializationConventionPlugin"
        }
        register("hilt") {
            id = "authapplication.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidFeature") {
            id = "authapplication.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("jvmLibrary") {
            id = "authapplication.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
