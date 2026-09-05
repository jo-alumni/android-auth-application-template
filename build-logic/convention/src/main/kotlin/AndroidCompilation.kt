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
