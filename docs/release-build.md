# releaseビルドとR8

学習用テンプレートの価値は「いつクローンしても、書かれている通りに動く」ことにある。
release ビルドの最適化(R8によるminify・obfuscation・リソース圧縮)を有効化したまま放置すると、
「型安全Navigationが壊れる」「DataStoreが読めなくなる」という実務で最も嵌まりやすい事故を、
CIもテストも検知できないまま抱え続けることになる。このドキュメントは、
何を・なぜ有効化し、どのkeepルールが・なぜ必要だったかをまとめる。

## 有効化した設定

`app/build.gradle.kts` で、AGP 9.3+ の `optimization` DSL を使って release ビルドの
最適化を有効化している。

```kotlin
buildTypes {
    release {
        optimization {
            enable = true
        }
    }
}
```

この1行で **コード最適化(旧 `isMinifyEnabled`)とリソース最適化(旧 `isShrinkResources`)の
両方が有効になる**。旧DSLのように `isMinifyEnabled` / `isShrinkResources` / `proguardFiles()` を
個別に書く必要はない。デフォルトのAndroid keepルール(`proguard-android-optimize.txt` 相当)も
自動的に含まれる。

keepルールは `app/src/main/keepRules/rules.keep`(`.keep` 拡張子、`src/<variant>/keepRules/`
配下)に置く。AGPがこのディレクトリ配下のファイルを自動的に結合してR8へ渡すため、
`proguardFiles()` での明示的な参照は不要。

## 実際に壊れることを確認した箇所: Protobuf Lite (`AuthPrefs`)

`:data` の `AuthPrefsSerializer` は、protobuf-lite が生成した `AuthPrefs`
(`com.google.protobuf.GeneratedMessageLite` のサブクラス)をDataStoreの永続化スキーマに使う。
`GeneratedMessageLite` は `newMessageInfo()` が生成する情報文字列に埋め込まれた
フィールド名(`"authToken_"`)をもとに、リフレクションで実フィールドを解決する。

keepルールを足す前の状態で `./gradlew :app:assembleRelease` を実行し、
`app/build/outputs/mapping/release/mapping.txt` を確認したところ、実際に

```
java.lang.String authToken_ -> e
```

とフィールド名がリネームされていた。この状態で実行すると、DataStoreの読み書き時に
`NoSuchFieldException` で落ちる(=「DataStoreが読めなくなる」)。

`tink-android` や `androidx.datastore:datastore-preferences` は、それぞれが shade/内蔵する
`GeneratedMessageLite` サブクラス向けの consumer rules を **自分のconsumer-rules.proとして
バンドルしている**(`app/build/outputs/mapping/release/configuration.txt` で確認できる)。
しかし、これらは「ライブラリ自身が生成するクラス」しか保護しない。本プロジェクト自身が
`protobuf-javalite` から生成する `AuthPrefs` は、どのライブラリの consumer rules にも
含まれないため、無防備だった。

対応として、`rules.keep` に次のルールを追加し、`authToken_` フィールド名がリネームされない
ことを再度 `mapping.txt` / `seeds.txt` で確認済み(`seeds.txt` にフィールドがkeepルートとして
記載される)。

```proguard
-keepclassmembers class com.example.authapplication.data.auth.proto.** extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}
```

## 確認した上でkeepルールを追加しなかった箇所

### kotlinx.serialization(`@Serializable` な Route)

Navigation Compose の型安全ナビゲーション(`navigation-compose` 2.9.8)は、
`composable<DetailRoute>` のように呼び出し時点で型引数が確定しているため、
`mapping.txt` を確認する限り `DetailRoute.serializer()` への直接呼び出しにコンパイルされる。
`DetailRoute` のフィールド(`itemId`)自体はリネームされるが、シリアライザは生成コードから
直接そのフィールドへアクセスするため、実行時のリフレクション解決を経由しない
(=フィールド名のリネームは実害がない)。

ただし将来 sealed class による多態的なRouteを追加するなど、実行時にシリアライザを
リフレクション解決する経路が増えた場合に備え、kotlinx.serialization公式README推奨の
keepルールを本プロジェクトのパッケージに絞って `rules.keep` に追加してある
(現状の機能に対しては必須ではない防御的なルール)。

### Hilt/Dagger・Tink

`./gradlew :app:assembleRelease` は追加のkeepルール無しで成功しており、
`configuration.txt` にはTink自身がバンドルする consumer rules
(shaded protobufのフィールド保護)のみが含まれる。HiltもTinkのAEAD登録
(`AeadConfig.register()`)も、文字列ベースのリフレクションではなく直接のオブジェクト生成/
生成コード経由で動作するため、追加のkeepルールは不要と判断した。

## `./gradlew assembleRelease` はCIで検証する

このIssueが問題視していたのは「release最適化を無効化したまま、実際には検証されていない」
状態そのものだった。同じ状態に戻らないよう、`.github/workflows/ci.yml` の `build` ジョブに
`assembleRelease` を追加し、R8/keepルールの構成が壊れたら通常のPRで検知できるようにしている。

## CI/静的解析だけでは検証できないこと(手動確認が必要)

`./gradlew assembleRelease` の成功は「ビルドが通ること」しか保証しない。
R8はリフレクション経由の呼び出しを静的解析できないため、keepルールが不足していても
ビルドエラーにはならず、**実行時にクラッシュ、または無言で誤動作する**。
そのため、次の項目は実機/エミュレータへのインストールでの目視確認が必要
(このリポジトリでCIに計装テストを載せない方針は [docs/ci.md](ci.md) 参照。
release変数のインストール確認はその方針の対象外で、人手のQAとして別途行う)。

- [ ] ログイン → ホーム/検索/お気に入り の3画面を行き来できる
- [ ] 各画面から詳細画面へ遷移できる
- [ ] ログアウト → 再起動してもログイン状態が正しく復元される(またはログイン画面に戻る)
- [ ] アプリを完全終了 → 再起動してもお気に入りの状態が保持されている(DataStoreの読み書き確認)
- [ ] `adb shell am start -a android.intent.action.VIEW -d "authapplication://detail/1"` で
      ディープリンクから詳細画面に直接遷移できる
- [ ] TopAppBarのデバッグメニューから「エラーを発生させる」→リトライの一連の流れが動く

## 未署名/デバッグ署名でのビルド手順

このリポジトリには release 用の `signingConfig` を定義していない(意図的)。
そのため `assembleRelease` の成果物は未署名APKになる。実機で動作確認する場合は、
デバッグ鍵で署名してからインストールする。

```bash
# 1. release ビルド(R8 minify 有効)
./gradlew assembleRelease

# 2. デバッグ鍵で署名する(Android Studio / Android SDKが生成する ~/.android/debug.keystore を使う)
$ANDROID_HOME/build-tools/<installed-version>/apksigner sign \
    --ks ~/.android/debug.keystore \
    --ks-pass pass:android \
    --out app/build/outputs/apk/release/app-release-debug-signed.apk \
    app/build/outputs/apk/release/app-release-unsigned.apk

# 3. 実機/エミュレータにインストールする
adb install app/build/outputs/apk/release/app-release-debug-signed.apk
```

debug署名のAPKは配布物ではなく手元検証専用なので、CIの成果物や配布チャネルには含めない。
