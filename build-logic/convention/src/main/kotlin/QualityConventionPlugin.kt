import com.diffplug.gradle.spotless.SpotlessExtension
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import dev.detekt.gradle.extensions.FailOnSeverity
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/**
 * ktlint(Spotless経由)とdetekt(compose-rules付き)を全モジュールへ一括で適用する。
 *
 * ルートの build.gradle.kts に一度だけ適用する。各モジュールの build.gradle.kts には書かせない。
 * モジュールを追加したときに静的解析だけ適用漏れになる、という事故を構造的に防ぐため。
 *
 * ルートプロジェクトはKotlinソースを持たないので、代わりにincluded buildである
 * build-logic のソースを解析対象に加える。Convention Plugin自身も同じ規約で書くため。
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        check(target == target.rootProject) {
            "authapplication.quality はルートの build.gradle.kts にだけ適用する（現在: ${target.path}）"
        }

        target.allprojects {
            configureSpotless()
            configureDetekt()
        }
    }
}

/** ktlintによるフォーマット検査。`./gradlew spotlessApply` で自動修正できる。 */
private fun Project.configureSpotless() {
    pluginManager.apply("com.diffplug.spotless")

    val ktlintVersion = rootProject.libs.findVersion("ktlint").get().requiredVersion
    // ktlintの設定はルートの .editorconfig に集約する。明示的に渡さないと
    // Spotlessはktlint既定のcode style(ktlint_official)で整形してしまう。
    val editorConfig = rootProject.layout.projectDirectory.file(".editorconfig").asFile

    extensions.configure<SpotlessExtension> {
        kotlin {
            if (isRootProject()) {
                target(rootProject.fileTree("build-logic") { include("**/src/**/*.kt") })
            } else {
                target("src/**/*.kt")
            }
            ktlint(ktlintVersion).setEditorConfigPath(editorConfig)
        }
        kotlinGradle {
            if (isRootProject()) {
                target(
                    rootProject.fileTree(".") { include("*.gradle.kts") },
                    rootProject.fileTree("build-logic") { include("**/*.gradle.kts") },
                )
            } else {
                target("*.gradle.kts")
            }
            ktlint(ktlintVersion).setEditorConfigPath(editorConfig)
        }
    }
}

/**
 * detektによる静的解析。Composeまわりの規約(Modifier引数の位置・デフォルト値、Previewの可視性など)は
 * compose-rules のルールセットに任せる。
 */
private fun Project.configureDetekt() {
    pluginManager.apply("dev.detekt")

    val detektConfig = rootProject.layout.projectDirectory.file("gradle/detekt/detekt.yml")
    val buildLogicSources = rootProject.layout.projectDirectory.dir("build-logic/convention/src")

    extensions.configure<DetektExtension> {
        // 既定のルールセットを土台にし、detekt.ymlには「既定から変えたところ」だけを書く。
        buildUponDefaultConfig.set(true)
        config.setFrom(detektConfig)
        // 既定(Error)のままだと、多くのルールが出すWarningでCIが落ちない。
        // 指摘は0件で保つ方針なのでWarningから落とす（docs/ci.md 参照）。
        failOnSeverity.set(FailOnSeverity.Warning)
        parallel.set(true)
        // baselineは設定しない。既存の指摘を凍結せずその場で直す方針のため。
        if (isRootProject()) {
            source.setFrom(buildLogicSources)
        }
    }

    dependencies {
        add("detektPlugins", rootProject.libs.findLibrary("detekt-rules-compose").get())
    }

    tasks.withType<Detekt>().configureEach {
        jvmTarget.set("11")
        // 指摘はコンソール出力で読む。HTMLだけは詳細確認用に残す。
        reports {
            html.required.set(true)
            checkstyle.required.set(false)
            sarif.required.set(false)
            markdown.required.set(false)
        }
    }
}

private fun Project.isRootProject(): Boolean = this == rootProject
