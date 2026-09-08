import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Compose Compiler Gradle Plugin適用 + buildFeatures.compose有効化 + Compose共通依存を提供する。
 * あわせて、Screen Composable単体のUIテストをJVM(Robolectric)上で実行するための設定も行う。
 *
 * 注意: この plugin 自身は com.android.library / com.android.application を適用しない。
 * 必ず authapplication.android.library / authapplication.android.application を
 * 先に適用したモジュールから利用すること（android拡張が未登録だと configure が失敗する）。
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<CommonExtension> {
                // buildFeaturesはCommonExtensionではラムダDSLを持たないため、プロパティに直接代入する。
                buildFeatures.compose = true

                // RobolectricはテーマやレイアウトなどのAndroidリソースを解決するため、
                // ユニットテストのクラスパスにマージ済みリソースを含める。
                testOptions.unitTests.isIncludeAndroidResources = true

                // Robolectricの起動SDKなどの設定をモジュールごとに重複させないよう、
                // ルートの gradle/robolectric を全モジュール共通のテストリソースとして読み込む。
                sourceSets.getByName("test").resources.srcDir(rootDir.resolve("gradle/robolectric"))
            }

            dependencies {
                add("implementation", platform(libs.findLibrary("androidx-compose-bom").get()))
                add("implementation", libs.findLibrary("androidx-compose-ui").get())
                add("implementation", libs.findLibrary("androidx-compose-ui-graphics").get())
                add("implementation", libs.findLibrary("androidx-compose-ui-tooling-preview").get())
                add("implementation", libs.findLibrary("androidx-compose-material3").get())
                add("implementation", libs.findLibrary("androidx-navigation-compose").get())

                // Composeを有効にしたモジュールでは、実機/エミュレータ無しでUIテストを書けるようにする。
                add("testImplementation", platform(libs.findLibrary("androidx-compose-bom").get()))
                add("testImplementation", libs.findLibrary("junit").get())
                add("testImplementation", libs.findLibrary("robolectric").get())
                add("testImplementation", libs.findLibrary("androidx-compose-ui-test-junit4").get())
                // createComposeRule() がテスト中に起動する ComponentActivity を提供する。
                add("debugImplementation", libs.findLibrary("androidx-compose-ui-test-manifest").get())
            }
        }
    }
}
