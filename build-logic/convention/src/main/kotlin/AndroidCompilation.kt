import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project

/**
 * Android Application/Libraryモジュール共通のcompileSdk/minSdk/Java互換性設定。
 * AndroidApplicationConventionPlugin / AndroidLibraryConventionPlugin から呼び出す。
 *
 * namespace, targetSdk, versionCode/versionName, testInstrumentationRunner等の
 * モジュール固有の設定は、このヘルパーでは扱わず各モジュールのbuild.gradle.ktsに残す。
 */
internal fun Project.configureAndroidCommon(
    commonExtension: CommonExtension,
) {
    commonExtension.apply {
        compileSdk {
            version = release(37)
        }

        // defaultConfig/compileOptionsはCommonExtensionではラムダDSLを持たず、
        // プロパティとして公開されているため直接代入する。
        defaultConfig.minSdk = 29

        compileOptions.sourceCompatibility = JavaVersion.VERSION_11
        compileOptions.targetCompatibility = JavaVersion.VERSION_11
    }
}

/**
 * Android Lintの共通設定。Application/Libraryの両Convention Pluginから呼び出す。
 *
 * 指摘はbaselineで凍結せず、その場で直す方針にしている。そのため
 * `lint-baseline.xml` は作らず、代わりに警告もビルドエラーとして扱う。
 * 「時間が経つだけで増える指摘」(依存の新しいバージョンが出た等)だけは
 * CIを恒常的に赤くするため無効化し、依存の更新はIssue側で扱う
 * (方針の詳細は docs/ci.md 参照)。
 */
internal fun configureLint(commonExtension: CommonExtension) {
    commonExtension.lint.apply {
        // 警告を放置できないようにする。抑制したいものは理由付きで個別にdisableする。
        warningsAsErrors = true
        abortOnError = true
        // テストコードも同じ基準で検査する。
        checkTestSources = true
        disable += setOf(
            // 依存/AGPの新しいバージョンが出るたびに指摘が増える。更新はIssueで扱う。
            "GradleDependency",
            "AndroidGradlePluginVersion",
        )
    }
}
