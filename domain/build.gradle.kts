plugins {
    alias(libs.plugins.kotlin.jvm)
    id("java-test-fixtures")
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)

    // FakeAuthRepository/MainDispatcherRuleなど、:app・:app:feature:login等の
    // 複数モジュールから共通で使うテスト用フェイクを testFixtures として公開する。
    testFixturesApi(libs.junit)
    testFixturesApi(libs.kotlinx.coroutines.core)
    testFixturesApi(libs.kotlinx.coroutines.test)
}
