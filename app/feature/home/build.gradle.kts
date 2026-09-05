plugins {
    id("authapplication.android.library")
    id("authapplication.android.compose")
    id("authapplication.android.feature")
}

android {
    namespace = "com.example.authapplication.feature.home"
}

dependencies {
    implementation(projects.app.core)
    implementation(projects.domain)
}
