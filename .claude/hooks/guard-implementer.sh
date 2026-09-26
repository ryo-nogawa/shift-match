#!/bin/bash
# implementer サブエージェントの禁止事項（.claude/agents/implementer.md）を機械的に強制する PreToolUse フック。
# pom.xml は deny せず、settings.json の ask でユーザーに確認する（依存の追加は Todo に明記されたものだけ）。
# メインエージェントからの呼び出しは対象外（agent_type が implementer のときだけ判定する）。
set -euo pipefail

input=$(cat)
agent_type=$(jq -r '.agent_type // empty' <<<"$input")
[[ "$agent_type" == "implementer" ]] || exit 0

tool_name=$(jq -r '.tool_name' <<<"$input")

deny() {
  echo "implementer では禁止されている操作です：$1" >&2
  exit 2
}

case "$tool_name" in
  Bash)
    command=$(jq -r '.tool_input.command // empty' <<<"$input")
    if grep -Eq '(^|[;&|[:space:]])git[[:space:]]+(push|switch|checkout|reset|rebase|merge)([[:space:]]|$)' <<<"$command"; then
      deny "git push・ブランチ切り替え・履歴を書き換える Git 操作"
    fi
    if grep -Eq '(^|[;&|[:space:]])gh[[:space:]]' <<<"$command"; then
      deny "gh コマンド（Issue・PR の操作はメインエージェントが行います）"
    fi
    ;;
  Edit|Write)
    file_path=$(jq -r '.tool_input.file_path // empty' <<<"$input")
    case "$file_path" in
      */docs/*) deny "docs/ の仕様書の変更" ;;
    esac
    ;;
esac

exit 0
