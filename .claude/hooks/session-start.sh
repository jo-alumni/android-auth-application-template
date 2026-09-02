#!/bin/bash
set -euo pipefail

if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
  exit 0
fi

cd "$CLAUDE_PROJECT_DIR"

# Android Studio以外(このリモート環境)ではlocal.propertiesが無いとSDKパスが
# 解決できないため、環境変数からsdk.dirを生成する。
if [ -n "${ANDROID_HOME:-}" ] && [ ! -f local.properties ]; then
  echo "sdk.dir=${ANDROID_HOME}" > local.properties
fi

# Gradle Wrapper本体・ビルドスクリプトのプラグイン依存関係だけを事前に解決しておく。
# test/assemble/lintをフルで実行すると毎セッション起動のコストが大きすぎるため、
# ここでは行わず、実際に必要になったタイミングで各コマンドに任せる。
./gradlew help --stacktrace
