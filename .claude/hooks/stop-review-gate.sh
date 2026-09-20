#!/bin/bash
set -euo pipefail

cd "$CLAUDE_PROJECT_DIR"

FLAG=".claude/.needs_code_review"

# Write/Edit後に一度でも停止しようとしたら、まだレビューしていない変更が
# 残っているかをこのフラグで判定する。フラグを消費(削除)してから停止をブロックし、
# sub agentによるレビューを挟ませる。レビュー後の再度の停止試行ではフラグが
# 既に無いので素通りする。
if [ -f "$FLAG" ]; then
  rm -f "$FLAG"
  cat <<'EOF'
{
  "decision": "block",
  "reason": "コードの実装(Write/Edit)が行われましたが、まだsub agentによるコードレビューを受けていません。停止する前に、Agentツール(例: code-reviewスキル、またはsubagent_type=\"claude\"のAgentツール)を使って今回の実装差分をレビューさせてください。指摘があれば修正したうえで完了としてください。"
}
EOF
else
  echo '{}'
fi
