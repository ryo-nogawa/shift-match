# Todo: 雇用区分（Issue #38）Codex レビュー 1 ラウンド目の指摘対応

- Issue: #38
- ブランチ: feature/38-employment-type
- 版: v1
- 対象仕様: V-7、F-1、F-7、8 章（入力表の列）
- 作成日: 2026-09-26

## 前提

- 読むべきもの：`target/reviews/code-quality-review.md`、`docs/specifications.md`（4.1・4.2・8 章）、`.claude/rules/tdd.md`、`.agents/rules/test.md`
- `docs/` は編集しない（仕様は既に正しい）。テストの期待値を仕様と異なる値へ書き換えない
- Todo ファイルは `- [ ]` を `- [x]` に変える以外の編集をしない。途中で報告せず最後まで進める
- 1 件ごとにコミット（Conventional Commits・日本語、末尾に `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>`）

## Todo

- [x] **R1. 区分が未送信の POST を V-7 エラーにする（MUST）**
  - 依頼事項：`EmployeeForm#employmentType`（`src/main/java/com/example/shiftmatch/controller/EmployeeForm.java:20`）が `"FULL_TIME"` で初期化されているため、氏名ありで `employees[N].employmentType` が送られない POST が V-7 を通過する。フィールドの初期値を外して `null` にし、未送信を V-7 エラーにする。初期値が「常勤」であるべき箇所（`ShiftController#index` の空行補充、POST で行が 0 件のとき補う空行、`shift-form.js` の行追加）は、明示的に `FULL_TIME` を設定する。既存の POST テストで区分パラメーターを省略しているものには、意図した区分を明示する（期待値は変えない）
  - 対象ファイル：`EmployeeForm.java`、`ShiftController.java`、`src/main/resources/static/js/shift-form.js`（行追加が `FULL_TIME` を選択していることの確認）、`ShiftControllerTest.java`
  - 完了条件：
    - テストを先に書き、RED（区分なしの POST が算出・保存されてしまう）を確認した
    - `@DisplayName` に `[V-7]` を含む「氏名あり・区分パラメーターなしの POST がエラー行番号つきで表示され、`save` も `assign` も呼ばれない」テストがある
    - 氏名なしの行で区分がなくてもエラーにならないテストがある
    - `GET /` の空行の区分が `FULL_TIME`（選択済み）であるテストが引き続き成功する
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
- [x] **R2. 入力表の見出しに「区分」列を追加する（MUST）**
  - 依頼事項：`src/main/resources/templates/index.html` の入力表の `<thead>` に `<th>区分</th>` を「氏名」と「休み」の間へ追加し、見出しと本文がともに 7 列で、順序が「（並べ替え）・氏名・区分・休み・開始・終了・（削除）」になるようにする
  - 対象ファイル：`index.html`、`ShiftControllerTest.java`
  - 完了条件：
    - テストを先に書き、RED を確認した
    - `<thead>` の見出しが 7 つで、順序が上記のとおりであることを確かめるテストがある（`[F-1]` を DisplayName に含める）
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
- [x] **R3. 区分の選択状態を検証するテストに直す（SHOULD）**
  - 依頼事項：`ShiftControllerTest` の `initializeAllEmploymentTypesToFullTimeWhenNoSave`・`restoresEmploymentTypesFromRepository`・`preservesEmploymentTypeSelectionAfterError` が、`select` 名や `option` の存在しか確かめていない。行ごとに対象の `select` の範囲を HTML から取り出し、期待する `option` だけに `selected` が付くことを検証する。不要な 1 回目の POST と未使用の `captureEmploymentTypeErrors`（871 行目付近）を削除する
  - 対象ファイル：`ShiftControllerTest.java`
  - 完了条件：
    - 3 つのテストが、行ごとの `selected` の位置を検証している（`selected` を外す・別の option に付ける変更を加えると失敗することを、一度試して確認する。確認後は元に戻す）
    - `captureEmploymentTypeErrors` と、使われていない 1 回目の POST が残っていない
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
- [x] **R4. 旧スキーマからの移行テストを実際の移行にする（SHOULD）**
  - 依頼事項：`SchemaTest#alterTableAddColumnWorksWithExistingData`（`src/test/java/com/example/shiftmatch/persistence/SchemaTest.java:96` 付近）を、`employment_type` 列のない旧定義の `saved_employee` を作って既存行を入れ、`schema.sql` の `ALTER TABLE saved_employee ADD COLUMN IF NOT EXISTS ...` を実際に適用したあと、既存行の区分が `FULL_TIME` になることを確認するテストに書き換える。`defaultEmploymentTypeIsFullTime` と重複する内容は削除する
  - 対象ファイル：`SchemaTest.java`
  - 完了条件：
    - 旧テーブルに既存行があり、移行後に `employment_type = 'FULL_TIME'` になることを検証している
    - `./mvnw test -Dtest=SchemaTest` が成功する
- [x] **R5. 全テストと静的解析を通す**
  - 依頼事項：`./mvnw spotless:apply` のあと `./mvnw test` を実行し、失敗・違反があれば直す
  - 完了条件：
    - `./mvnw test` が成功し、Spotless・Checkstyle の違反が 0 件である

## 実行ログ

<!-- implementer が試行結果を追記する欄 -->
