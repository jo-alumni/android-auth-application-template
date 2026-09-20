#!/bin/bash
set -euo pipefail

cd "$CLAUDE_PROJECT_DIR"

# Write/Editでコードが変更されたことを記録する。
# このフラグはStopフック(stop-review-gate.sh)が読み、
# 「実装後は必ずsub agentレビューを挟む」ことを強制する材料に使う。
mkdir -p .claude
touch .claude/.needs_code_review
