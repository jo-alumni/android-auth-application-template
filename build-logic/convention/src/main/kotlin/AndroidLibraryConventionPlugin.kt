import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                configureAndroidCommon(this)

                // マルチモジュールではリソース名がモジュール横断でマージされるため、
                // 同名のリソースがあると意図しない上書きが起きる。
                // モジュールごとに接頭辞を強制し、衝突を機械的に防ぐ。
                resourcePrefix = resourcePrefix()
            }
        }
    }
}

/**
 * モジュールのGradleパスから文字列リソース等の接頭辞を導く。
 *
 * - `:app:core` → `core_`
 * - `:app:feature:home` → `feature_home_`
 * - `:data` → `data_`
 *
 * 接頭辞を各モジュールのbuild.gradle.ktsに書かせるとモジュール追加時に付け忘れるため、
 * Convention Plugin側でパスから機械的に決める。
 */
private fun Project.resourcePrefix(): String =
    path.removePrefix(":app:").removePrefix(":").replace(':', '_') + "_"
