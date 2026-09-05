import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * :app:feature:* および :app 向けの共通設定。
 * Hilt + Serialization convention を内部適用したうえで、
 * ViewModel/Navigation/Hilt-Compose連携に必要な共通依存を追加する。
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("authapplication.hilt")
            pluginManager.apply("authapplication.kotlin.serialization")

            dependencies {
                add("implementation", libs.findLibrary("androidx-activity-compose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                add("implementation", libs.findLibrary("androidx-hilt-lifecycle-viewmodel-compose").get())
            }
        }
    }
}
