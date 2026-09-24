# Todo: 画面デザインを C 案（ウォーム配色のカード UI + 時間軸バー）へ刷新する

- Issue: #18
- ブランチ: feature/18-ui-redesign
- 版: v1
- 対象仕様: F-1, F-4, 7 章 出力仕様, 8 章 画面仕様
- 作成日: 2026-09-24

## 前提

- 読むべきドキュメント・規約：`docs/specifications.md`（7 章・8 章）、`.claude/rules/tdd.md`、`.agents/rules/test.md`・`formatting.md`・`comment.md`
- デザインの正：`.claude/design/proposal-c-timeline.html`（`<style>` の CSS とクラス名・マークアップが見た目の見本）。ただし末尾の「デモ用の状態切替バー」（`.demo-bar`）と、状態切替用の JS・`data-show`・`hidden` 属性は本番へ持ち込まない
- 変更してよいのは次のファイルのみ：`src/main/resources/templates/index.html`、`src/main/resources/static/css/shift-form.css`（新規）、`src/main/resources/static/js/shift-form.js`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`。Java 本体（Controller・Service・domain）、`pom.xml`、`docs/` は変更しない。新しい依存は追加しない
- **DOM 契約（変更禁止）**：`#employee-rows`（tbody）、その中の `tr`、`.delete-row-btn`、`#add-row-btn`、`name` 属性の連番（`employees[n].name` / `earlyWish` / `lateWish`）、`<form method="post" th:action="@{/shift}" th:object="${shiftForm}">`。入力行は `table > tbody > tr` のままにする（スマホ表示は CSS で行う）
- **既存テストを壊さない**：エラーメッセージ・不成立メッセージ・表の見出し（`氏名`・`勤務時間`・`休憩時間`）・`08:00〜17:00` / `12:00〜21:00` の文言は現状のまま維持する。既存テストは割当結果の表の各 `<tr>` に「早番」「遅番」が含まれないことを検証している。**時間軸バーは `<tr>` を使わず `<div>` で作り、表の行の中に「早番」「遅番」を書かない**（「早番」「遅番」は凡例にだけ書く。仕様 7 章で許可済み）
- テストの書き方は既存に合わせる（`@WebMvcTest(ShiftController.class)` + `MockMvc`、`@MockitoBean`、本文検証は `getContentAsString()` + `assertTrue`。Hamcrest は使わない）。`ShiftControllerTest` に新しい `@Nested` クラス（例：`UiDesignTest`）を作って追加する
- 時間軸バーの割合（Thymeleaf で算出。Java は変更しない）：表示範囲は 8:00〜21:00 の 13 時間（780 分）。`left(%) = (時 × 60 + 分 − 480) × 100 / 780`、`width(%) = (終了分 − 開始分) × 100 / 780`。数値は小数 2 桁・小数点は `.` に固定する（例：`${#numbers.formatDecimal(値, 1, 2, 'POINT')}`）。`LocalTime` は `breakTime.start().getHour()` / `getMinute()` で取り出す。例：休憩 12:00〜13:00 は `left:30.77%;width:7.69%`、早番の勤務 08:00〜17:00 は `left:0.00%;width:69.23%`、遅番の勤務 12:00〜21:00 は `left:30.77%;width:69.23%`
- 早番/遅番の判定は既存と同じ `stat.index < 2`（先頭 2 行が早番、残りが遅番）
- JavaScript は自動テストできないため、完了条件は `grep` と `node --check` で確認する（`node` は使用可能）

## Todo

- [x] **T1. CSS ファイルを作成し、`index.html` から読み込む（TDD）**
  - 依頼事項：`ShiftControllerTest` に、GET `/` の本文に `<link rel="stylesheet" href="/css/shift-form.css">`（属性の順序・空白の差は許容。`/css/shift-form.css` を指す stylesheet の link）が含まれるテストと、`/css/shift-form.css` がクラスパス上に存在するテスト（`new ClassPathResource("static/css/shift-form.css").exists()`）を追加する。RED を確認してから、`index.html` の `<head>` に `th:href="@{/css/shift-form.css}"` の link と `<meta name="viewport" content="width=device-width, initial-scale=1" />`、`<html lang="ja" ...>` を追加し、`static/css/shift-form.css` を空でない最小内容（コメント 1 行）で作成して GREEN にする。CSS の中身は T7 で書く
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`、`src/main/resources/templates/index.html`、`src/main/resources/static/css/shift-form.css`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `@DisplayName` の先頭に `[F-1]` を付けたテストが存在し、Given-When-Then で書かれている
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する

- [x] **T2. ページの骨格（ヘッダー・カード・ボタン）を C 案のクラス構成にする（TDD）**
  - 依頼事項：GET `/` の本文に、`class="app"` を持つ要素、`class="hero"` 内に `<h1>` の「シフト作成」と `class="eyebrow"`（文言 `Shift Match`）と `class="lead"`（文言は見本 HTML の `.lead` と同じ）、`class="card"` を持つ `<form>`、`class="btn ghost"` の「行を追加」ボタン（`id="add-row-btn"` を維持）、`class="btn primary"` の「シフトを作成」ボタンが含まれるテストを追加し、RED を確認してから `index.html` を見本 `proposal-c-timeline.html` のマークアップに合わせて変更する。従業員数の表示（`<b id="row-count">`）は、`shiftForm.employees.size()` を `th:text` で出力する。希望の凡例（`.wish-legend`）も見本どおり入れる。`<script>` の読み込みは維持する
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`、`src/main/resources/templates/index.html`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `@DisplayName` の先頭に `[F-1]` を付けたテストが存在する
    - `#add-row-btn` と `<form ... th:action="@{/shift}">` が維持されている（既存の DOM 契約テストが成功する）
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する

- [x] **T3. 入力行に `data-label`（スマホ表示用）と、選択値を示す `data-value` を付ける（TDD）**
  - 依頼事項：POST `/shift` で `employees[0].earlyWish=DESIRED`・`employees[0].lateWish=UNAVAILABLE` を送って入力エラー（例：氏名重複）で再表示させ、本文の該当行の早番 `<select>` に `data-value="DESIRED"`、遅番 `<select>` に `data-value="UNAVAILABLE"` が含まれるテストを追加する（再表示の作り方は既存の `[V-3]` などのテストを参考にする）。また GET `/` の各行の `<td>` に `data-label="氏名"`・`data-label="早番希望"`・`data-label="遅番希望"` が含まれ、`<select>` に `data-value=""` が付くことを検証する。RED を確認してから `index.html` の入力行を変更する。`data-value` は `th:attr="data-value=*{employees[__${stat.index}__].earlyWish}"` のように出力する（値が null のときは空文字になるよう確認する）。既存の `th:field` はそのまま維持する。入力表の見出しは見本どおり `<small>` で時間帯（`8:00〜17:00`・`12:00〜21:00`）を付ける
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`、`src/main/resources/templates/index.html`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `@DisplayName` の先頭に `[F-1]` を付けたテストが存在する
    - 異常系（入力エラーでの再表示）を含むテストが存在する
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する

- [ ] **T4. エラー・不成立・結果カードのマークアップを C 案にする（TDD）**
  - 依頼事項：入力エラー（`limitExceededError`・`duplicateErrors`・`wishErrors`）の各ブロックに `class="alert"` と `role="alert"` を付け、不成立表示を `class="card result"` 内の `class="empty"`（アイコン `<span class="empty-icon">！</span>`、メッセージ `条件を満たす組み合わせが見つかりませんでした。` は現状の文言を維持、補足 `<small>` を追加）にする。成立時は `class="card result"` 内に `class="score"`（`<span class="score-num">` にスコア、`<small> / 4</small>`）、既存の結果表に `class="result-table"`、勤務時間のセルは `<span class="pill early">` または `<span class="pill late">`（`stat.index < 2` で判定。**クラス名にだけ early/late を使い、表示文言に「早番」「遅番」は入れない**）、未出勤者は `class="unassigned"` 内に `<span class="chip">` で並べる（未出勤者が 0 名のときはブロックごと出さない現状を維持）。先に各クラスが本文に含まれるテスト（正常系：結果表示、不成立、入力エラーの 3 パターン）を書いて RED を確認する
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`、`src/main/resources/templates/index.html`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `@DisplayName` の先頭に `[F-4]` を付けたテストが存在する
    - 不成立・入力エラーの異常系のテストが存在し成功する
    - 既存の `[F-4]` 表ヘッダ・勤務時間・休憩時間のテストが変更なしで成功する
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する

- [ ] **T5. 割当結果に時間軸バーと凡例を表示する（TDD）**
  - 依頼事項：成立時、結果表の上に `<div class="timeline">` を出力する。中身は、時刻の目盛り `<div class="tl-axis">`（8・10・12・14・16・18・20 を `<span style="left:X%">` で並べる。X は `(時 − 8) × 100 / 13`、小数 2 桁）、割当 4 名分の `<div class="tl-row">`（`<span class="tl-name">` に氏名、`<div class="tl-track">` の中に勤務バー `<div class="tl-work early|late" style="left:..%;width:..%">` と休憩バー `<div class="tl-break" style="left:..%;width:..%">`）、凡例 `<p class="tl-legend">`（`<i class="lg early"></i>早番<i class="lg late"></i>遅番<i class="lg brk"></i>休憩`）。**`<tr>` は使わない**。行の順序は `assignmentResult.breakTimes()` の順（先頭 2 名が早番）。テストは `AssignmentResult` に既存テストと同様のモックを与え、①本文に `class="timeline"` と `class="tl-row"` が 4 つあること、②早番の勤務バーに `left:0.00%;width:69.23%`、遅番の勤務バーに `left:30.77%;width:69.23%` が含まれること、③凡例に「早番」「遅番」「休憩」が含まれること、④割当結果の表の各 `<tr>` に「早番」「遅番」が含まれないこと（既存テストと同じ観点）、⑤不成立のとき `class="timeline"` が出力されないこと（異常系）を検証する。休憩バーの位置は、モックの休憩時刻（`BreakTime` の値は `AssignmentResult.breakTimes()` の実装を読んで確認する）から算出した期待値で検証する
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`、`src/main/resources/templates/index.html`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `@DisplayName` の先頭に `[F-4]` を付けたテストが存在し、Given-When-Then で書かれている
    - 不成立時にタイムラインが出ないことを検証する異常系のテストが存在する
    - 既存の `[F-4]` テスト（表ヘッダ・勤務時間・「早番」「遅番」が行に含まれないこと）が変更なしで成功する
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する

- [ ] **T6. `shift-form.js` を新しい入力行の構造に合わせる**
  - 依頼事項：行の追加時に、見本 `proposal-c-timeline.html` の `<script>` と同様に、各 `<td>` へ `data-label`（`氏名`・`早番希望`・`遅番希望`）を付け、`<select>` に `data-value`（初期値は空文字）を付ける。`<select>` の `change` イベントで `data-value` を選択値に更新する（`#employee-rows` へのイベント委譲）。初期表示時（`DOMContentLoaded`）にも、既存の全 `<select>` の `data-value` を現在の選択値で設定する。従業員数の表示 `#row-count` を、初期表示・行追加・行削除のたびに現在の行数へ更新する。行削除後の `name` 属性の振り直し（`renumberInputIndices`）と、最後の 1 行の削除ボタン無効化は現状どおり維持する。デモ用の状態切替コードは持ち込まない。既存の DOM 生成コード（`createElement` の重複）は、`data-label` を付けるついでにヘルパー関数へまとめてよいが、`name` 属性の形式は変えない
  - 対象ファイル：`src/main/resources/static/js/shift-form.js`
  - 完了条件：
    - `node --check src/main/resources/static/js/shift-form.js` が成功する
    - `grep -n "data-label\|dataset.value\|row-count" src/main/resources/static/js/shift-form.js` で、`data-label`・`data-value` の設定と `row-count` の更新が存在する
    - `grep -n "employees\[" src/main/resources/static/js/shift-form.js` で、`employees[n].name` / `earlyWish` / `lateWish` の形式が維持されている
    - `grep -n "demo" src/main/resources/static/js/shift-form.js` が 0 件である
    - `./mvnw test` が成功する（JS の変更でテストが壊れていない）

- [ ] **T7. `shift-form.css` に C 案のスタイルを実装する**
  - 依頼事項：`proposal-c-timeline.html` の `<style>` から、`/* ---- デモ用の状態切替バー ---- */` 以降（`.demo-bar`・`[hidden]`）を除いた全 CSS を `src/main/resources/static/css/shift-form.css` へ移す（`:root` の変数、ダークモードの `@media (prefers-color-scheme: dark)`、スマホ用の `@media (max-width:640px)`、タイムライン関連を含む）。`index.html` で使っているクラス（`app`・`hero`・`eyebrow`・`lead`・`card`・`card-head`・`count`・`wish-legend`・`w d/a/u`・`input-table`・`actions`・`btn primary/ghost`・`alert`・`empty`・`empty-icon`・`score`・`score-num`・`score-label`・`result-table`・`pill early/late`・`unassigned`・`chip`・`timeline`・`tl-*`・`lg`）が CSS 内で定義されていることを確認し、足りないものは見本の定義に合わせて補う。デザインの見た目は見本と同じにする
  - 対象ファイル：`src/main/resources/static/css/shift-form.css`
  - 完了条件：
    - `grep -c "demo-bar" src/main/resources/static/css/shift-form.css` が 0 である
    - `grep -n "\.timeline\|\.tl-work\|\.tl-break\|select\[data-value=DESIRED\]\|max-width:640px\|prefers-color-scheme" src/main/resources/static/css/shift-form.css` で、タイムライン・希望の色分け・スマホ用・ダークモードの定義がそれぞれ存在する
    - `index.html` の `class="..."` に使われている各クラス名が、`shift-form.css` にセレクターとして存在する（確認に使った `grep` の結果を実行ログに残す）
    - `./mvnw test` が成功する

- [ ] **T8. 全テストと静的解析が成功することを確認する**
  - 依頼事項：`./mvnw spotless:apply` を実行してから `./mvnw test` を実行する。失敗があれば、原因を修正する（テストの期待値を仕様と異なる値へ書き換えない）。`git status` で、変更が前提に書いた 4 ファイル種別（`index.html`、`shift-form.css`、`shift-form.js`、`ShiftControllerTest.java`）と `.claude/todos/` 以外に及んでいないことを確認する
  - 対象ファイル：（変更なし。確認のみ）
  - 完了条件：
    - `./mvnw test` が成功する（テスト件数・Checkstyle 違反 0 件・Spotless 違反なしを実行ログに記録する）
    - Java 本体（`src/main/java/`）と `pom.xml` に main との差分がない（`git diff main --stat -- src/main/java pom.xml` で確認）。`docs/` は、既にコミット済みの 7 章の変更以外の差分がない

## 実行ログ

<!-- implementer が試行結果（失敗理由・リトライ回数）を追記する欄。作成時は空のままにする -->
