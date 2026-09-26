# Todo: 従業員入力行の並べ替え（▲▼ ボタン）

- Issue: #36（(3) の並べ替え機能。(1)(2) は実装済み）
- ブランチ: feature/36-result-view-and-reorder
- 版: v1
- 対象仕様: F-2, F-6, 新規 F-8（入力行の並べ替え）
- 作成日: 2026-09-26

## 前提

- 読むべきドキュメント・規約：`docs/specifications.md` 3 章（機能一覧）と 8 章（行削除の記述）、`.claude/rules/tdd.md`、`.agents/rules/test.md`、`.agents/rules/lambda.md`（メソッド参照禁止）
- 既存のテストは、`ShiftControllerTest` の `JavaScriptAddDeleteRows`（`readShiftFormJs()` で JS を文字列として読み、`contains` で確認）と、GET / の HTML を `contains` / 正規表現で確認する形式。JS の動作テスト基盤は無いので、新しい依存（Jest 等）は追加しない
- 行の name は `employees[N].name/.off/.start/.end` と隠しフィールド `_employees[N].off`。インデックスの振り直しは `shift-form.js` の `renumberInputIndices()` を再利用する
- 入力値・休みの disabled 状態は、行の DOM ごと移動すれば保たれる（`insertBefore` で行を移動する）
- サーバー側（Java の本体コード）の変更は不要
- 入力順は同点時の優先順位に使われる（AGENTS.md「同点時」）ため、順序が意味を持つ

## Todo

- [x] **T1. 入力行の HTML に ▲（上へ）▼（下へ）ボタンを追加する**
  - 依頼事項：`index.html` の各入力行（`th:each` の行、最後の `<td>`）の「削除」ボタンの前に、`<button type="button" class="move-up-btn" aria-label="上へ">▲</button>` と `<button type="button" class="move-down-btn" aria-label="下へ">▼</button>` を追加する。先にテストを書く（Red）。仕様 ID：F-8
  - 対象ファイル：`src/main/resources/templates/index.html`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `@DisplayName` が `[F-8]` で始まり Given-When-Then で書かれたテストが存在し、GET / の HTML に `move-up-btn` と `move-down-btn` が入力行の数だけ含まれることを確認している
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
- [x] **T2. JS で行を上下に移動し、インデックスを振り直す**
  - 依頼事項：`shift-form.js` の `employeeRows` のクリックハンドラで、`move-up-btn` は行を 1 つ前の行の前へ、`move-down-btn` は行を 1 つ後の行の後ろへ移動する（`insertBefore`）。移動後に `renumberInputIndices()` を呼ぶ。テスト（JS の文字列確認）を先に書く。仕様 ID：F-8
  - 対象ファイル：`src/main/resources/static/js/shift-form.js`、`ShiftControllerTest.java`
  - 完了条件：
    - `[F-8]` で始まるテストが、`shift-form.js` に `move-up-btn`・`move-down-btn`・`insertBefore` と、移動処理から `renumberInputIndices` を呼ぶ記述があることを確認している
    - RED を確認してから実装した
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
- [x] **T3. 先頭行の▲・末尾行の▼を無効化し、行の追加・削除・移動のたびに更新する**
  - 依頼事項：`shift-form.js` に `updateMoveButtonState()` を追加する。先頭行の `.move-up-btn` と末尾行の `.move-down-btn` を `disabled` にし、それ以外は有効にする。初期表示、行追加、行削除、行移動の各処理の末尾で呼ぶ。行追加（`addRowBtn` のハンドラ）で作る新しい行にも ▲▼ ボタンを削除ボタンの前に追加する。仕様 ID：F-8
  - 対象ファイル：`src/main/resources/static/js/shift-form.js`、`ShiftControllerTest.java`
  - 完了条件：
    - `[F-8]` で始まるテストが、`shift-form.js` に `updateMoveButtonState` の定義と、動的に作る行の `move-up-btn`・`move-down-btn` の生成があることを確認している
    - RED を確認してから実装した
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
- [x] **T4. ボタンの CSS を追加する**
  - 依頼事項：`shift-form.css` で `.move-up-btn`・`.move-down-btn` を `.delete-row-btn` と同系統の見た目（小さめ・枠線・ホバー・`:disabled` で薄く・`:focus-visible`）にする。ボタンが横に並んでも折り返さないよう、最後のセルを `white-space: nowrap` にする。スマホ幅（640px 以下）でも崩れないよう、`.input-table td:last-child` の既存指定を確認する。テストは不要（見た目のみ）
  - 対象ファイル：`src/main/resources/static/css/shift-form.css`
  - 完了条件：
    - CSS に `.move-up-btn`・`.move-down-btn` の定義があり、`:disabled` と `:focus-visible` のスタイルが含まれる
    - `./mvnw test` が成功する
- [x] **T5. 仕様書とドキュメントを更新する**
  - 依頼事項：`docs/specifications.md` の機能一覧に `F-8 | 入力行の並べ替え | 各入力行の ▲▼ ボタンで従業員の順番を入れ替えます` を追加し、8 章の行削除の記述の後に、並べ替えの記述（先頭行の▲・末尾行の▼は無効、移動後に `name` のインデックスを 0 から連番に振り直す、入力順は同点時の優先順位に影響する）を追加する。`README.md` に入力行の並べ替えができる旨があれば 1 行追記する
  - 対象ファイル：`docs/specifications.md`、`README.md`
  - 完了条件：
    - `docs/specifications.md` に `F-8` の行と、並べ替えの記述がある
    - `./mvnw test` が成功する（Spotless・Checkstyle を含む）
- [x] **T6. 全テストと静的解析を確認する**
  - 依頼事項：`./mvnw spotless:apply` の後に `./mvnw test` を実行し、結果を報告する
  - 対象ファイル：なし
  - 完了条件：
    - `./mvnw test` で全テストが成功し、Checkstyle の違反が 0 件である

## 実行ログ

- T5 試行 1/4：失敗 — `docs/` の仕様書の変更は implementer では禁止事項。T5 の完了条件は仕様書ファイルの編集を要求していますが、implementer のルールで `docs/` の変更が禁止されているため実装不可
- T5 は implementer の権限外（docs/ 編集禁止）のため、メインエージェントが実施した
