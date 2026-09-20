# CIと静的解析

学習用テンプレートの価値は「いつクローンしても、書かれている通りに動く」ことにある。
ビルドとテストがCIで常時検証されていないと、依存の更新やAGPのバージョン変更で静かに壊れる。
また、`.claude/rules/` に書いた規約も、CIで強制されて初めて規約として機能する。

ここでは何をどこで検査しているか、なぜその形にしたかをまとめる。

## 全体像

| 検査 | コマンド | CIのジョブ | 担当範囲 |
| --- | --- | --- | --- |
| ビルド | `./gradlew assembleDebug` | `build / test / lint` | コンパイルが通ること |
| ユニットテスト（Robolectric含む） | `./gradlew test` | `build / test / lint` | ViewModel・UseCase・Screen Composable |
| Android Lint | `./gradlew lint` | `build / test / lint` | Android固有の問題（未使用リソース、リソース名の接頭辞など） |
| フォーマット（ktlint） | `./gradlew spotlessCheck` | `ktlint / detekt` | 改行・空白・import順・トレイリングカンマ |
| 静的解析（detekt + compose-rules） | `./gradlew detekt` | `ktlint / detekt` | 複雑度・命名・Composeの書き方 |

実機/エミュレータが要る計装テスト（`./gradlew connectedAndroidTest`）はCIに載せていない。
1画面で完結する確認はRobolectricで `src/test` に寄せてあるため
（[.claude/rules/testing.md](../.claude/rules/testing.md)）、CIは `./gradlew test` だけで回る。

ワークフローは [.github/workflows/ci.yml](../.github/workflows/ci.yml)。
PRと `main` へのpushで動き、ジョブを「ビルド・テスト・Lint」と「静的解析」に分けている。
失敗したときに、チェック名だけで「壊れたのか」「規約に反したのか」を切り分けられるようにするため。

## ドキュメントのみの変更はCIをskipする

`docs/*.md` や `CLAUDE.md`、`.claude/rules/*.md` のような変更はビルド・テスト・静的解析の
結果を左右しない。そうした変更でもAndroid SDKのセットアップを含むフルビルドが毎回走るのは
無駄なので、`changes` ジョブで変更対象を判定し、コードに影響しないと分かれば
`build` / `static-analysis` をskipする。

- 判定は [dorny/paths-filter](https://github.com/dorny/paths-filter) の `code` フィルタで行う。
  「`**` にマッチさせたあと、ドキュメント系パスを否定パターンで除外する」形で、
  除外リスト以外のファイルが1つでも変更されていれば `code` を `true` にする。
  - `**/*.md` … `README.md` / `CLAUDE.md` / `docs/**/*.md` / `.claude/rules/*.md` を含む
    全Markdown。
  - `docs/**` … `docs/` 配下に将来Markdown以外のファイルが増えても拾えるよう保険で明記。
  - `.claude/**` … Claude Code用のフック・設定・ルールドキュメント一式。Gradleビルドには
    一切関与しない。
  - [.editorconfig](../.editorconfig) は**意図的に除外しない**。ktlintが直接読む設定ファイルで
    `spotlessCheck` の結果を左右するため、コード変更として扱う。
  - `.github/workflows/ci.yml` 自体の変更も除外しない。ワークフローを変えたときは
    常にフルで検証する。
- 粒度はモジュール単位ではなく「コードに影響するか/しないか」の二値にとどめている。
  `spotlessCheck` / `detekt` は前述の通り `QualityConventionPlugin` が `allprojects` へ
  一括適用する設計が前提になっており、モジュール単位で対象を絞る細粒度の最適化は
  この前提と衝突する。ビルド時間の最適化よりも「例外を作らない」設計を優先する。
- ワークフロー自体を `paths-ignore` で起動させない方式は採っていない。
  ワークフローが起動しないとチェックが1件も投稿されず、branch protectionで
  このCIをrequired status checksに設定していた場合、PRが永久にpending扱いになり
  マージできなくなるおそれがあるため。`changes` ジョブに対する `needs:` +
  ジョブ単位の `if` でskipすれば、GitHub Actions上は「skipped」という結果が投稿され、
  required status checksとしては合格扱いになる。
- 既知のトレードオフとして、`changes` ジョブ自体が失敗した場合は `needs:` の既定動作により
  `build` / `static-analysis` も実行されずskip扱いになる。「判定不能ならフル実行する」という
  フェイルセーフは入れていない。

## 全モジュールへの適用はConvention Pluginで行う

ktlintとdetektは、各モジュールの `build.gradle.kts` には**書かない**。
ルートの `build.gradle.kts` に `authapplication.quality`
（[QualityConventionPlugin](../build-logic/convention/src/main/kotlin/QualityConventionPlugin.kt)）を
1回だけ適用し、そこから全プロジェクトへ適用する。

モジュールごとに書く方式だと、モジュールを追加したときに静的解析だけ適用漏れになる。
「新しい画面は既存の画面をそのまま模写すれば書ける」状態を保つため、
モジュール側の `build.gradle.kts` には `namespace` とモジュール固有の依存だけを残す。

ルートプロジェクトはKotlinソースを持たないので、代わりに included build である
`build-logic`（Convention Plugin自身）を解析対象にしている。
`./gradlew spotlessCheck detekt` でリポジトリ内のKotlinコードが漏れなく検査される。

## ktlint（Spotless経由）

- 設定はルートの [.editorconfig](../.editorconfig) に集約する。ktlintはこのファイルを直接読むため、
  CIとAndroid Studioで同じ結果になる。
- コードスタイルは `intellij_idea`。`gradle.properties` の `kotlin.code.style=official` と揃えている。
  ktlint既定の `ktlint_official` は改行位置まで強く規定し、既存コードとの差分が大きすぎるため採らない。
- `function-signature` / `class-signature` は無効にしている。
  引数が1つでも複数行で縦に並べる、という既存の書き方を1行へ畳み直させないため。
- `ktlint()` には `.editorconfig` のパスを明示的に渡している。渡さないとSpotlessは
  ktlint既定のコードスタイルで整形してしまい、`.editorconfig` の指定が効かない。
- 違反は `./gradlew spotlessApply` で自動修正できる。

## detekt（+ compose-rules）

- 設定は [gradle/detekt/detekt.yml](../gradle/detekt/detekt.yml)。
  `buildUponDefaultConfig = true` なので、書くのは「既定から変えたところ」だけ。
- Composeまわりの規約は [compose-rules](https://mrmans0n.github.io/compose-rules/) に任せる。
  Modifier引数の位置・デフォルト値（`ModifierWithoutDefault` / `ComposableParamOrder`）、
  Preview関数の命名と可視性（`PreviewNaming` / `PreviewPublic`）、
  CompositionLocalの許可リスト（`CompositionLocalAllowlist`）などが対象。
  `PreviewNaming` を有効にしているのは、
  [.claude/rules/compose-preview.md](../.claude/rules/compose-preview.md) の
  「対象のComposable名 + `Preview`」という命名を機械的に強制するため。
- フォーマット系のルール（detekt-formatting）は入れない。ktlintと二重指摘になるため、
  改行・空白・import順はktlintの担当と決めている。
- `failOnSeverity` を `Warning` にしている。既定（`Error`）のままだと、
  多くのルールが出すWarningでCIが落ちず、指摘が溜まっていくため。
- detektは安定版の1.23.xではなく2.0.0系を使っている。1.23.xはKotlin 1.9系のコンパイラを内蔵しており、
  `gradle/gradle-daemon-jvm.properties` で指定しているJDK 25上では起動できない
  （`java.lang.IllegalArgumentException: 25.0.3`）。

## 指摘はbaselineで凍結せず、その場で直す

ktlint・detekt・Android Lintのいずれについても、baseline
（`lint-baseline.xml` / `detekt-baseline.xml`）は作らない。

baselineは「今ある指摘を無視する」仕組みなので、一度作ると
「なぜこの指摘が残っているのか」が誰にも分からないまま残り続ける。
規約を機械的に強制するために静的解析を入れたのに、
その規約に反したコードが最初から例外として登録されている状態は本末転倒になる。

そのかわり、次の2つを守る。

1. **Android Lintは警告もエラーとして扱う**（`lint.warningsAsErrors = true` / `abortOnError = true`。
   設定は [AndroidCompilation.kt](../build-logic/convention/src/main/kotlin/AndroidCompilation.kt) の
   `configureLint`）。テストコードも同じ基準で検査する（`checkTestSources = true`）。
2. **抑制するときは、その場に理由を書く**。ルールごと無効化するのではなく、
   `@Suppress("RestrictedApi")` のように対象を絞り、なぜ抑制してよいのかをコメントで残す。

唯一、ルールごと無効化しているのは「時間が経つだけで増える指摘」だけ。
`GradleDependency` と `AndroidGradlePluginVersion` は、新しいバージョンが公開されるたびに
指摘が増えてCIが恒常的に赤くなる。依存の更新は「CIを通すための作業」ではなく
独立した作業なので、Issue側で扱う。

## JDKの構成

- Gradle Daemon自体のJVMは `gradle/gradle-daemon-jvm.properties` でJDK 25に固定されている。
- 各モジュールのコンパイルはJDK 11ターゲット（`jvmToolchain(11)` / `compileOptions`）。
- CIでは `actions/setup-java` で11と25の両方を入れ、`gradle.properties` の
  `org.gradle.java.installations.fromEnv` でGradleに見つけさせている。
  指定しないとGradleがtoolchainを見つけられず、foojay-resolverが毎回JDKをダウンロードする。

## 手元で回すとき

```bash
# CIと同じ内容
./gradlew assembleDebug test lint
./gradlew spotlessCheck detekt

# フォーマット違反を自動修正する
./gradlew spotlessApply
```
