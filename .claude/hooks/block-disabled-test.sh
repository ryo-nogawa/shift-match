#!/bin/bash
# テストを @Disabled で回避することを禁止する PreToolUse フック（.claude/rules/tdd.md の禁止事項）。
set -euo pipefail

input=$(cat)
file_path=$(jq -r '.tool_input.file_path // empty' <<<"$input")

case "$file_path" in
  */src/test/*) ;;
  *) exit 0 ;;
esac

new_text=$(jq -r '.tool_input.new_string // .tool_input.content // empty' <<<"$input")
if grep -q '@Disabled' <<<"$new_text"; then
  echo "失敗するテストを @Disabled で回避することは禁止されています（.claude/rules/tdd.md）。実装を修正してください。" >&2
  exit 2
fi

exit 0
