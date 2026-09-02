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

# Gradle Wrapper本体と、ビルド/テストで使う依存関係を事前に解決しておくことで、
# 以降の ./gradlew タスク実行時のネットワーク待ちを減らす。
./gradlew testDebugUnitTest assembleDebug lint --stacktrace
