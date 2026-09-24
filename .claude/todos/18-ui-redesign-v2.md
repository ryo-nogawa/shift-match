# Todo: 画面デザインを C 案（ウォーム配色のカード UI + 時間軸バー）へ刷新する

- Issue: #18
- ブランチ: feature/18-ui-redesign
- 版: v2
- 対象仕様: F-1, F-4, F-5, 7 章 出力仕様, 8 章 画面仕様
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

### 前回（v1）の失敗理由と今回の変更点

- 前回（v1）の失敗理由：T4〜T8 が大きすぎ、`implementer` が T3 までで停止した（T1〜T3 は完了しコミット済み：b99c6ea, 19338ef, b78fb5d）
- 今回（v2）の変更点：残りを 8 件（T4〜T11）に分割した。T2 で `index.html` のマークアップ（`alert`・`empty`・`score`・`pill`・`chip`）が先に実装済みのため、T4・T6 は「既存の実装を固定するテストの追加」とし、T5 は不足分（補足文）だけを RED から実装する
- **1 回の依頼で全部を進めようとせず、1 Todo ごとにコミットしてから次へ進むこと**。途中で止まる場合は、コミット済みの Todo を `[x]` にして報告する
- 現状の `index.html` には次が既にある：ページ骨格、入力行（`data-label`・`data-value=""`）、`.alert` 3 種（`role="alert"`）、不成立の `.card.result > .empty > .empty-icon + p`、成立時の `.score`（`.score-num`・`.score-label`）、`.result-table`（`.pill.early` / `.pill.late`）、`.unassigned > .chip`。**まだ無いもの**：不成立の補足 `<small>`、時間軸バー `.timeline`、`shift-form.css` の中身（現在はコメント 1 行のみ）、`#row-count` の JS 更新
- 現状の `shift-form.js` は `data-label`・`data-value` の付与と更新までは実装済み。`employees[n].*` の振り直しも維持されている
- 休憩時刻は実装が固定値：早番 1 人目 13:00〜14:00、2 人目 14:00〜15:00、遅番 1 人目 15:00〜16:00、2 人目 16:00〜17:00（`AssignmentResult.breakTimes()`）。バーの割合（`left` / `width`）の期待値は次のとおり
  - 早番の勤務：`left:0.00%;width:69.23%`、遅番の勤務：`left:30.77%;width:69.23%`
  - 休憩（幅はすべて `width:7.69%`）：13:00 → `left:38.46%`、14:00 → `left:46.15%`、15:00 → `left:53.85%`、16:00 → `left:61.54%`
  - 目盛り `left`：8→`0.00%`、10→`15.38%`、12→`30.77%`、14→`46.15%`、16→`61.54%`、18→`76.92%`、20→`92.31%`

## Todo

- [x] **T1. CSS ファイルを作成し、`index.html` から読み込む（TDD）**
  - 依頼事項：（v1 で完了。`/css/shift-form.css` の link・viewport・`lang="ja"`・CSS ファイルの存在テスト）
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`、`src/main/resources/templates/index.html`、`src/main/resources/static/css/shift-form.css`
  - 完了条件：
    - v1 で完了済み（b99c6ea）

- [x] **T2. ページの骨格（ヘッダー・カード・ボタン）を C 案のクラス構成にする（TDD）**
  - 依頼事項：（v1 で完了）
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`、`src/main/resources/templates/index.html`
  - 完了条件：
    - v1 で完了済み（19338ef）

- [x] **T3. 入力行に `data-label` と `data-value` を付ける（TDD）**
  - 依頼事項：（v1 で完了）
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`、`src/main/resources/templates/index.html`、`src/main/resources/static/js/shift-form.js`
  - 完了条件：
    - v1 で完了済み（b78fb5d）

- [ ] **T4. 入力エラー 3 種が `.alert`（`role="alert"`）で表示されることをテストで固定する**
  - 依頼事項：`ShiftControllerTest` の `UiDesignTest`（T2 で作成済みの `@Nested`。無ければ新規作成）に、`[V-1][F-1]` などの既存テストと同様の入力で、次の 3 テストを追加する。①上限超過（`limitExceededError`）、②氏名の重複（`duplicateErrors`）、③希望の不正値（`wishErrors`）のそれぞれで、本文に `class="alert"` と `role="alert"` が含まれる。再現のさせ方は、既存の同種テスト（重複は `shiftAssignmentService.findDuplicateNames` をモック、希望の不正値は `employees[0].earlyWish` に不正値を送る等）を参考にする。**マークアップは実装済みなので RED にならない**。代わりに、各テストが検出力を持つことを、`index.html` の該当 `class="alert"` を一時的に消して失敗することで確認し、確認後に元へ戻す（コミットには含めない）
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `@DisplayName` の先頭に `[F-1]` を付けたテストが 3 件（3 種のエラーそれぞれ）存在し、Given-When-Then で書かれている
    - 一時的に `class="alert"` を消すと 3 件とも失敗することを確認した（実行ログに記録する）
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する

- [ ] **T5. 不成立表示に補足文を追加する（TDD）**
  - 依頼事項：`UiDesignTest` に、不成立（`shiftAssignmentService.assign(...)` が `Optional.empty()` を返す）のとき、本文に `class="card result"`、`class="empty"`、`class="empty-icon"`、既存の文言 `条件を満たす組み合わせが見つかりませんでした。`、補足文 `希望（×）を見直すか、従業員を追加してください。`（`<small>` 内）が含まれるテストを追加する。補足文が無いので RED になることを確認してから、`index.html` の `.empty` 内、メッセージ `<p>` の直後に `<small>希望（×）を見直すか、従業員を追加してください。</small>` を追加して GREEN にする。あわせて、成立時には `class="empty"` が出力されないことを検証するテスト（異常系側）も追加する
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`、`src/main/resources/templates/index.html`
  - 完了条件：
    - テストを先に書き、補足文の欠如でアサーションが失敗（RED）することを確認した
    - `@DisplayName` の先頭に `[F-5]` を付けたテストが存在する
    - 成立時に `class="empty"` が出ないことを検証するテストが存在する
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する

- [ ] **T6. 成立時の結果カード（スコア・表・ピル・未出勤者チップ）をテストで固定する**
  - 依頼事項：`UiDesignTest` に、成立時（既存テストと同様に `AssignmentResult` を作る。早番: 太郎・花子、遅番: 次郎・美咲、スコア 3、未出勤者: 五郎）のテストを追加する。①`class="score-num"` の中にスコア `3` と ` / 4` が含まれる、②`class="result-table"` が含まれる、③`pill early` が 2 つ・`pill late` が 2 つ含まれる（本文中の出現回数を数える）、④`class="unassigned"` の中に `class="chip"` があり `五郎` が含まれる、⑤未出勤者が 0 名（`List.of()`）のとき `class="unassigned"` が含まれない（異常系側）。**マークアップは実装済みなので RED にならない**。T4 と同様に、`index.html` の該当クラスを一時的に外して失敗することで検出力を確認し、確認後に元へ戻す（コミットには含めない）
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `@DisplayName` の先頭に `[F-4]` を付けたテストが ①〜⑤の各観点について存在する
    - 一時的にクラスを外すと該当テストが失敗することを確認した（実行ログに記録する）
    - 既存の `[F-4]` テスト（表ヘッダ・勤務時間・「早番」「遅番」が行に含まれないこと）が変更なしで成功する
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する

- [ ] **T7. 時間軸バー本体（勤務バー・休憩バー）を表示する（TDD）**
  - 依頼事項：`UiDesignTest` に、T6 と同じ成立時のモックで、次を検証するテストを追加する。①本文に `class="timeline"` が 1 つ、`class="tl-row"` がちょうど 4 つ、②各 `tl-name` に `太郎`・`花子`・`次郎`・`美咲` が順に含まれる、③`tl-work early` のスタイルが `left:0.00%;width:69.23%`（2 件）、`tl-work late` のスタイルが `left:30.77%;width:69.23%`（2 件）、④`tl-break` のスタイルが `left:38.46%;width:7.69%`・`left:46.15%;width:7.69%`・`left:53.85%;width:7.69%`・`left:61.54%;width:7.69%` の 4 件、⑤不成立のとき `class="timeline"` が含まれない。RED を確認してから、`src/main/resources/templates/index.html` の成立時カード内、`<table class="result-table">` の**直前**に `<div class="timeline">` を出力する。構造は見本 `.claude/design/proposal-c-timeline.html` の `.tl-row`（`tl-name` の `<span>`、`tl-track` の `<div>`、その中に `tl-work` と `tl-break` の `<div>`）と同じ。ループは `th:each="breakTime, stat : ${assignmentResult.breakTimes()}"`。割合は `${#numbers.formatDecimal(式, 1, 2, 'POINT')}` で小数 2 桁・小数点 `.` に固定する（`th:style="|left:${...}%;width:${...}%|"`）。式は、勤務の開始分・終了分を `stat.index < 2 ? 480 : 720` / `stat.index < 2 ? 1020 : 1260`（8:00・12:00 / 17:00・21:00 の分換算）とし、`left = (開始分 - 480) * 100.0 / 780`、`width = (終了分 - 開始分) * 100.0 / 780`。休憩は `breakTime.start().getHour() * 60 + breakTime.start().getMinute()`。**`<tr>` は使わず `<div>` で作る**（表の行に「早番」「遅番」を入れないため）。この Todo では目盛り・凡例は作らない
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`、`src/main/resources/templates/index.html`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `@DisplayName` の先頭に `[F-4]` を付けたテストが存在し、Given-When-Then で書かれている
    - 不成立時にタイムラインが出ないことを検証する異常系のテストが存在する
    - 既存の `[F-4]` テスト（表ヘッダ・勤務時間・「早番」「遅番」が行に含まれないこと）が変更なしで成功する
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する

- [ ] **T8. 時間軸の目盛りと凡例（早番・遅番・休憩）を表示する（TDD）**
  - 依頼事項：`UiDesignTest` に、成立時のモックで、次を検証するテストを追加する。①`class="tl-axis"` が含まれ、目盛りの `left` が `0.00%`・`15.38%`・`30.77%`・`46.15%`・`61.54%`・`76.92%`・`92.31%` の 7 つ（文字は `8`・`10`・`12`・`14`・`16`・`18`・`20`）、②`class="tl-legend"` の中に `早番`・`遅番`・`休憩` が含まれ、`class="lg early"`・`class="lg late"`・`class="lg brk"` が含まれる、③割当結果の表（`<table class="result-table">` から `</table>` まで）に「早番」「遅番」が含まれない。RED を確認してから、`src/main/resources/templates/index.html` の `.timeline` 内に、見本 `.claude/design/proposal-c-timeline.html` の `.tl-axis`（先頭、7 つの `<span style="left:X%">`）と `.tl-legend`（末尾）を実装する。目盛りは `th:each="h : ${#numbers.sequence(8, 20, 2)}"`、`left = (h - 8) * 100.0 / 13`（小数 2 桁）で出力する。凡例の文言は `<i class="lg early"></i>早番<i class="lg late"></i>遅番<i class="lg brk"></i>休憩`
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`、`src/main/resources/templates/index.html`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `@DisplayName` の先頭に `[F-4]` を付けたテストが存在する
    - 割当結果の表の中に「早番」「遅番」が含まれないことを検証するテストが存在する
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する

- [ ] **T9. `shift-form.js` の残作業（`#row-count` 更新・重複コード整理）**
  - 依頼事項：①`#row-count` の表示を、初期表示（`DOMContentLoaded`）・行追加・行削除のたびに現在の行数（`employeeRows.querySelectorAll("tr").length`）へ更新する関数 `updateRowCount()` を追加し、既存の `updateDeleteButtonState()` と同じ箇所から呼ぶ。②行追加時の `<select>` 生成（早番・遅番でほぼ同じコードが 2 回ある）を、`createWishSelect(name)`（`name` 属性・`data-value=""`・4 つの `<option>`（`-- 未選択 --`・`◎ 希望`・`○ 可能`・`× 不可`、value は空・`DESIRED`・`AVAILABLE`・`UNAVAILABLE`）を返す）にまとめる。`name` の形式（`employees[n].name` / `earlyWish` / `lateWish`）、`data-label`、`data-value`、削除後の `renumberInputIndices()`、最後の 1 行の削除ボタン無効化は変えない。デモ用のコードは持ち込まない
  - 対象ファイル：`src/main/resources/static/js/shift-form.js`
  - 完了条件：
    - `node --check src/main/resources/static/js/shift-form.js` が成功する
    - `grep -n "row-count" src/main/resources/static/js/shift-form.js` で `row-count` の更新が存在する
    - `grep -c "createElement(\"option\")" src/main/resources/static/js/shift-form.js` の結果が 4 以下である（重複していた 8 箇所がまとまっている）
    - `grep -n "employees\[" src/main/resources/static/js/shift-form.js` で `name` の形式が維持されている
    - `grep -n "demo" src/main/resources/static/js/shift-form.js` が 0 件である
    - `./mvnw test` が成功する

- [ ] **T10. `shift-form.css` に C 案のスタイルを実装する**
  - 依頼事項：`.claude/design/proposal-c-timeline.html` の `<style>` から、`/* ---- デモ用の状態切替バー ---- */` 以降（`.demo-bar`・`[hidden]`）を除いた全 CSS を、`src/main/resources/static/css/shift-form.css` へ移す（先頭のコメント 1 行は残してよい）。`:root` の変数、ダークモード（`@media (prefers-color-scheme: dark)`）、スマホ用（`@media (max-width:640px)`）、タイムライン関連（`.timeline`・`.tl-*`・`.lg`）、希望の色分け（`select[data-value=DESIRED]` など）をすべて含める。`index.html` の `class="..."` に使われている各クラスが CSS に定義されていることを確認し、足りなければ見本の定義に合わせて補う。**この CSS は見本の見た目を再現するためのもので、値を変えない**
  - 対象ファイル：`src/main/resources/static/css/shift-form.css`
  - 完了条件：
    - `grep -c "demo-bar" src/main/resources/static/css/shift-form.css` が 0 である
    - `grep -n "\.timeline\|\.tl-work\|\.tl-break\|select\[data-value=DESIRED\]\|max-width:640px\|prefers-color-scheme" src/main/resources/static/css/shift-form.css` で、タイムライン・希望の色分け・スマホ用・ダークモードの定義がそれぞれ存在する
    - `index.html` で使うクラス（`app`・`hero`・`eyebrow`・`lead`・`card`・`card-head`・`count`・`wish-legend`・`input-table`・`actions`・`btn`・`primary`・`ghost`・`alert`・`empty`・`empty-icon`・`score`・`score-num`・`score-label`・`result-table`・`pill`・`early`・`late`・`unassigned`・`chip`・`timeline`・`tl-axis`・`tl-row`・`tl-name`・`tl-track`・`tl-work`・`tl-break`・`tl-legend`・`lg`・`delete-row-btn`）が、すべて CSS にセレクターとして存在する（確認に使った `grep` を実行ログに残す）
    - `./mvnw test` が成功する

- [ ] **T11. 全テストと静的解析が成功することを確認する**
  - 依頼事項：`./mvnw spotless:apply` を実行してから `./mvnw test` を実行する。失敗があれば原因を修正する（テストの期待値を仕様と異なる値へ書き換えない）。`git status` で、変更が前提に書いた 4 ファイル種別（`index.html`、`shift-form.css`、`shift-form.js`、`ShiftControllerTest.java`）と `.claude/todos/` 以外に及んでいないことを確認する
  - 対象ファイル：（変更なし。確認のみ）
  - 完了条件：
    - `./mvnw test` が成功する（テスト件数・Checkstyle 違反 0 件・Spotless 違反なしを実行ログに記録する）
    - Java 本体（`src/main/java/`）と `pom.xml` に main との差分がない（`git diff main --stat -- src/main/java pom.xml` で確認）。`docs/` は、既にコミット済みの 7 章の変更以外の差分がない

## 実行ログ

<!-- implementer が試行結果（失敗理由・リトライ回数）を追記する欄。作成時は空のままにする -->
