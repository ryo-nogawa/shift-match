# Todo: Codex レビュー 1 ラウンド目の指摘対応（画面デザイン刷新）

- Issue: #18
- ブランチ: feature/18-ui-redesign
- 版: v1
- 対象仕様: F-4, F-5, 6 章（T-3 決定済み：不成立は事実のみ表示）、7 章
- 作成日: 2026-09-24

## 前提

- 読むべきドキュメント・規約：`docs/specifications.md`（6 章の T-3、7 章）、`target/reviews/code-quality-review.md`（指摘の全文）、`.claude/rules/tdd.md`、`.agents/rules/test.md`・`formatting.md`
- 変更してよいのは次のファイルのみ：`src/main/resources/templates/index.html`、`src/main/resources/static/css/shift-form.css`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`、`.claude/design/proposal-a-clean.html`、`.claude/design/proposal-b-dark-glass.html`。Java 本体・`pom.xml`・`docs/` は変更しない
- 背景：不成立表示に補足文 `<small>希望（×）を見直すか、従業員を追加してください。</small>` を追加したが、仕様 6 章（T-3）は「原因の分析・表示は行わず、不成立の事実のみを示す」と決めているため仕様違反（MUST 指摘）。補足文を削除する
- 1 Todo が終わるたびに、チェックと実行ログを更新して Conventional Commits 形式（日本語）でコミットしてから次へ進む
- `./mvnw test` は通常環境で成功する（ローカルでは 68 件成功）。失敗した場合は、失敗理由を実行ログに書いて報告する

## Todo

- [x] **T1. 不成立表示から補足文を削除する（TDD）**
  - 依頼事項：`ShiftControllerTest` の `UiDesignTest` にある `shouldDisplaySupplementalTextInEmptyMessage`（`[F-5]`、約 900〜926 行目）を、補足文が**出力されない**ことを検証するテストへ変更する。メソッド名を `shouldDisplayOnlyUnassignableMessageWithoutSupplementalText`、`@DisplayName` を `[F-5] Given: 条件を満たす組み合わせがないとき, When: POST /shift を実行すると, Then: 不成立の事実のみが表示され、補足文や対応案は表示されないこと` に変更し、①`class="card result"`・`class="empty"`・`class="empty-icon"`・`条件を満たす組み合わせが見つかりませんでした。` が含まれる（既存の検証は維持）、②`希望（×）を見直すか、従業員を追加してください。` が含まれない、③`class="empty"` の要素（`class="empty"` から対応する `</div>` まで）に `<small>` が含まれない、を検証する。先にテストだけ変更し、②③で失敗（RED）することを確認してから、`src/main/resources/templates/index.html` の 100 行目付近の `<small>希望（×）を見直すか、従業員を追加してください。</small>` の行を削除して GREEN にする
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`、`src/main/resources/templates/index.html`
  - 完了条件：
    - テストを先に変更し、アサーションで失敗（RED）することを確認した
    - `@DisplayName` の先頭に `[F-5]` を付けたテストが存在し、補足文が含まれないことを検証している
    - `grep -rn "見直すか" src/` が 0 件である
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する

- [x] **T2. `.unassigned` の折り返しを有効にする**
  - 依頼事項：`src/main/resources/static/css/shift-form.css` の `.unassigned`（25 行目付近、`display: flex` のルール）に `flex-wrap: wrap` を追加する。それ以外の値は変えない。未出勤者が多数いるとき（最大 16 名）でもチップがカード外にあふれないようにするため
  - 対象ファイル：`src/main/resources/static/css/shift-form.css`
  - 完了条件：
    - `grep -n "\.unassigned" src/main/resources/static/css/shift-form.css` の該当ルールに `flex-wrap: wrap` が含まれる
    - `./mvnw test` が成功する

- [x] **T3. タイムラインの氏名と順序を厳密に検証するテストへ強化する**
  - 依頼事項：`ShiftControllerTest` の `shouldDisplayTimelineWithCorrectRows`（約 1177 行目）の最後の `assertTrue(body.contains("tl-name") && ...)` を置き換える。レスポンス本文から `class="timeline"` の開始位置以降で、`class="tl-name"` を持つ要素の中身を、`Pattern.compile("<span class=\"tl-name\">([^<]*)</span>")` の `Matcher` で順に抽出し（結果表など `timeline` の外の氏名を拾わないよう、`class="timeline"` から `class="result-table"` の開始位置までを `substring` で切り出してから抽出する）、`List.of("太郎", "花子", "次郎", "美咲")` と `assertEquals` で一致することを検証する。`tl-name` の実際の出力形式は `src/main/resources/templates/index.html` を読んで確認し、正規表現を合わせる。実装は変更しない（既存実装で GREEN になる）。**検出力の確認**：`index.html` の `tl-name` の出力を一時的に別の従業員の名前に変える（または順序を入れ替える）と、このテストが失敗することを確認し、確認後に元へ戻す（コミットには含めない）
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `@DisplayName` の記載（「tl-name に太郎・花子・次郎・美咲が順に含まれる」）と検証内容が一致している
    - 一時的に `tl-name` の出力を変えると失敗することを確認した（実行ログに記録する）
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する

- [x] **T4. `UiDesignTest` の重複コードをヘルパーメソッドへまとめる（リファクタリング）**
  - 依頼事項：`ShiftControllerTest` の `UiDesignTest`（836 行目〜）内で繰り返されている ①`AssignmentResult`（早番: 太郎・花子、遅番: 次郎・美咲、スコア 3、未出勤者: 五郎）の生成、②`shiftAssignmentService.findDuplicateNames(any())` が空リストを返し `assign(any())` が指定の結果を返すスタブ設定、③4 人分（または 1 人分）の POST パラメータの構築、を `UiDesignTest` 内の private ヘルパーメソッド（例：`createAssignmentResult()`、`stubAssignSuccess(AssignmentResult)`、`stubAssignEmpty()`、`createValidParams()`）へ抽出し、各テストから呼ぶ。**各テストの `@DisplayName`・期待値・検証内容は一切変えない**（テストの件数も変えない）。完全修飾名（`com.example.shiftmatch.domain.AssignmentResult` など）の繰り返しは、ヘルパーの中に閉じ込めるか `import` にまとめてよい。抽出前後で `./mvnw test -Dtest=ShiftControllerTest` の件数が同じであることを確認する
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `./mvnw test -Dtest=ShiftControllerTest` の実行件数が、リファクタリング前と同じで、すべて成功する（前後の件数を実行ログに記録する）
    - `grep -c "new com.example.shiftmatch.domain.AssignmentResult" src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java` が、リファクタリング前より減っている（前後の数値を実行ログに記録する）
    - `@DisplayName` の文言が変更されていない（`git diff` で `@DisplayName` の行に差分がない。T1 と T3 で意図して変えたものを除く）

- [x] **T5. デザイン案 HTML の行末空白を削除する**
  - 依頼事項：`.claude/design/proposal-a-clean.html` の 132 行目と `.claude/design/proposal-b-dark-glass.html` の 131 行目の行末空白（trailing whitespace）を削除する。それ以外は変更しない
  - 対象ファイル：`.claude/design/proposal-a-clean.html`、`.claude/design/proposal-b-dark-glass.html`
  - 完了条件：
    - `git diff --check main` が成功する（出力なし）

- [ ] **T6. 全テストと静的解析が成功することを確認する**
  - 依頼事項：`./mvnw spotless:apply` を実行してから `./mvnw test` を実行する。失敗があれば原因を修正する（テストの期待値を仕様と異なる値へ書き換えない）。`git status` で未コミットの差分がないことを確認する
  - 対象ファイル：（変更なし。確認のみ）
  - 完了条件：
    - `./mvnw test` が成功する（テスト件数・Checkstyle 違反 0 件・Spotless 違反なしを実行ログに記録する）
    - `git diff --check main` が成功する
    - Java 本体（`src/main/java/`）と `pom.xml` に main との差分がない（`git diff main --stat -- src/main/java pom.xml` で確認）

## 実行ログ

- T3 検出力確認：期待値を一時的に `["太郎", "太郎", "次郎", "美咲"]` に変更してテストを実行し、失敗することを確認した（エラーメッセージで実際の出力 `[太郎, 花子, 次郎, 美咲]` が正しく検出された）。その後、期待値を正しい値に戻して成功を確認した。
- T4 リファクタリング前後の件数：リファクタリング前は `AssignmentResult` 生成が 17 回、後は 6 回（削減 65%）。UiDesignTest のテスト件数は 17 件でリファクタリング前後で変更なし。@DisplayName の文言も変更なし。
