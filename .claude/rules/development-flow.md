# 開発フロー

コーディングエージェントは、以下のフィードバックループを**自律的に**回します。途中経過をその都度ユーザーへ確認する必要はありません。完成した時点でユーザーへ提出（報告）します。

ただし、次の場合はループを止めてユーザーへ確認します。

- 仕様が曖昧・未決（TBD）で、推測でしか進められないとき
- 仕様そのものを変える必要があると判断したとき
- 新しい依存ライブラリを追加したいとき

## ループ

### 1. Issue と PR を起票する

Issue と PR の作成は自分で `gh` を実行せず、Codex をヘッドレスモード（`codex exec`）で起動して依頼します。Codex には `.agents/skills/` のスキルを使わせます。

- `gh` が GitHub と通信できるように、`workspace-write` サンドボックスでネットワークを許可して起動します（`codex exec` は既定だと読み取り専用で、ネットワークも使えません）
- テンプレートの選択と本文の作成はスキル側の定義に従います。このファイルにテンプレートの中身を重ねて書かないでください
- Codex の最終出力から Issue・PR の URL と番号を確認し、以降の手順で使います

#### Issue

`create-issue` スキル（`.agents/skills/create-issue/SKILL.md`）を使います。

```bash
codex exec -s workspace-write -c sandbox_workspace_write.network_access=true \
  '$create-issue 次の内容で機能追加の Issue を起票してください：<依頼内容・対応する仕様 ID>'
```

#### PR

作業ブランチに最初のコミットを push した後（手順 2 の後）、`create-pr` スキル（`.agents/skills/create-pr/SKILL.md`）を使って **Draft** で作成します。

```bash
codex exec -s workspace-write -c sandbox_workspace_write.network_access=true \
  '$create-pr main を比較先として Draft PR を起票してください（gh pr create --draft）。対応する Issue は #<Issue番号> です。'
```

- スキルが見つからない、または起票できなかった場合は、推測で自分が代わりに起票せず、Codex の出力（理由と下書き）を添えてユーザーへ確認します

### 2. 最新の main から作業ブランチを作成する

```bash
git switch main
git pull origin main
git switch -c <prefix>/<issue番号>-<概要>   # 例：feature/12-input-validation
```

- `<prefix>` は `feature` / `fix` / `refactor` / `docs` / `chore` から選びます

### 3. TDD で実装する

- [tdd.md](tdd.md) のルールに従い、Red → Green → Refactor を繰り返します
- コミットは意味のある単位（1 サイクルごとなど）で行い、メッセージは Conventional Commits 形式（日本語）で書きます
- 実装はサブエージェントに任せます
  1. `create-todo` スキルで Todo リスト（`.claude/todos/*.md`）を作成します
  2. `implementer` サブエージェントに Todo ファイルのパスを渡して実装を依頼します
  3. 【完了】の報告を受けたら、Todo の全チェックと `./mvnw test` の結果を確認して手順 4 へ進みます
  4. 【未完了】の報告を受けたら、`create-todo` スキルを再作成モードで実行して Todo リストを作り直し、2 に戻ります。作り直しは最大 2 回（v3 まで）で、それでも未完了ならユーザーへ確認します
  5. コミットは `implementer` が Todo 1 件ごとに行います（Todo ファイルも Git 管理対象としてコミットします）

### 4. 全テストと静的解析を実行する

```bash
./mvnw test
```

- `./mvnw test` は `validate` フェーズにバインドされた Checkstyle（`maven-checkstyle-plugin`）を自動的に含みます。設定は [config/checkstyle/checkstyle.xml](../../config/checkstyle/checkstyle.xml) を参照してください
- テストが 1 件でも失敗した場合、または Checkstyle の違反が 1 件でもある場合は手順 3 に戻ります
- Checkstyle の詳細は `target/checkstyle-result.xml` で確認できます
- Maven はシステムの `mvn` ではなく、必ず Maven Wrapper（`./mvnw`）を使用します

### 5. Codex にコードレビューを依頼する

`.codex/skills/code-review/SKILL.md` のレビュースキル（`code-review`）を使い、main との差分を Codex にレビューさせます。

```bash
codex exec --sandbox workspace-write '$code-review main'
```

- `codex exec` の既定の sandbox は read-only で、`target/reviews/` へレポートを保存できないため、`--sandbox workspace-write` を必ず付けます

- レビューの観点・出力形式はスキル側の定義に従います。このファイルに観点を重ねて書かないでください
- 詳細レポートは `target/reviews/code-quality-review.md`（コード品質）と `target/reviews/security-risk-review.md`（セキュリティー）に保存されます。Codex の最終出力だけでなく、これらのレポートも読んで指摘内容を確認します
- 手順 6 では MUST の指摘は必ず対応します。SHOULD・WANT は要否を判断し、対応しない場合は理由を記録します
- スキルが見つからない、またはレビューが完了しなかった場合は、推測でレビューを進めずにユーザーへ確認します

### 6. 指摘を修正する

- 指摘内容を確認し、対応が必要なものを修正します
  - 振る舞いの変更を伴う修正は、手順 3 の TDD（Red から）でやり直します
  - 対応しない指摘は、その理由を記録しておき、最終報告に含めます
- 修正後は**手順 4（全テストと静的解析）から**やり直し、再度 Codex レビューを受けます
- 対応が必要な指摘がなくなるまで繰り返します。ただし、Codex レビューは **最大 3 ラウンド** までとし、3 ラウンド目でも対応が必要な指摘が残る場合はループを止め、残った指摘と各ラウンドの対応内容をユーザーへ報告して判断を仰ぎます

### 7. ユーザーへ報告する

- PR 本文のテスト結果を最新の値に更新し、Draft を解除します

  ```bash
  git push
  gh pr ready
  ```

- ユーザーへは次の内容を報告します
  - Issue と PR の URL
  - 実装内容の要約（対応した仕様 ID を含む）
  - 実行したコマンドとその結果（成功・失敗、テスト件数、Checkstyle の違反件数）
  - Codex レビューの指摘と対応状況（対応しなかった指摘はその理由）
