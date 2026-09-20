// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.protobuf) apply false
    // Convention Plugin(authapplication.quality)が全モジュールへ適用するため、
    // ここではクラスパスに載せるだけにする。
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.detekt) apply false

    // ktlint / detekt を全モジュールに適用する。詳細は docs/ci.md を参照。
    id("authapplication.quality")
}
