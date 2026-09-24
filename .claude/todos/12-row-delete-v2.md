# Todo: 入力表の行削除機能（F-6）を実装する

- Issue: #12
- ブランチ: feature/12-row-delete
- 版: v2
- 対象仕様: F-6（新規）, F-2（関連）, 8 章 画面仕様
- 作成日: 2026-09-24

## 前提

- 前回（v1）の失敗理由：T1 は `docs/` の変更だが、implementer はフック（`.claude/hooks/guard-implementer.sh`）で `docs/` の変更を禁止されており実行できなかった
- 今回の変更点：T1 はメインエージェントが実施済み（`[x]`）。implementer は **T2 以降のみ** 実装する。`docs/` は変更しない

- 読むべきドキュメント・規約：
  - `docs/specifications.md`（3 章 機能一覧、8 章 画面仕様。特に「インデックスに欠番があると正しくバインドできません」の注意書き）
  - `.claude/rules/tdd.md`（Red → Green → Refactor を厳守）
  - `.agents/rules/` のうち作業に関係するもの（`test.md`、`formatting.md` など）
  - 既存の `src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java` のテストの書き方（`@WebMvcTest` + `MockMvc`、`@MockitoBean`、本文検証は `getContentAsString()` + `Assertions.assertTrue(body.contains(...))`。Hamcrest は使わない）
- 設計方針：
  - **F-6 は新規仕様**。ユーザーが「行の削除機能」の実装を明示的に指示済み（Issue #12）。仕様書に F-6 を追加する作業を T1 で行う
  - **削除ボタン**：各入力行の末尾セルに `<button type="button" class="delete-row-btn">削除</button>` を置く。初期表示（Thymeleaf の `th:each`）の行にも、JavaScript で追加する行にも同じ構造で付ける。表ヘッダにも空の `<th>` を 1 つ追加して列数を揃える
  - **最低 1 行は残す**：入力表が 1 行のときは、その行の削除ボタンを `disabled` にする（行数が変わるたびに全行の状態を更新する）
  - **インデックスの振り直し**：削除後、`employees[N].name` / `.earlyWish` / `.lateWish` の `N` を、上から 0 始まりの連番に振り直す（`name` 属性を `/^employees\[\d+\]/` で置換する）。行追加時のインデックスも「現在の行数」で決まるため、削除後でも欠番・重複が出ない
  - **JavaScript は素の JavaScript**（フレームワーク・ライブラリ・npm は導入しない）。行の削除はイベント委譲（`employee-rows` の `click`）で実装し、動的に追加した行にも効くようにする
  - **サーバー側（Controller・Service）は変更しない**。V-1〜V-4 の仕様は変更しない
  - **新しい依存ライブラリは追加しない**。`pom.xml` は変更しない
  - JavaScript の単体テスト基盤はないため、T3 のブラウザ挙動は Todo 内で示す手順で手動確認し、結果を実行ログに残す

## Todo

- [x] **T1. 仕様書に F-6（行の削除）を追加する**
  - 依頼事項：`docs/specifications.md` の 3 章の機能一覧表に `F-6 | 入力行の削除 | 入力行を削除します（最低 1 行は残します）` の行を追加する。8 章には、行の削除機能の仕様（各行に「削除」ボタンを置くこと、最低 1 行は残すこと、削除後に `name` 属性のインデックスを 0 から連番に振り直すこと）を、既存の注意書きの直後に短く追記する。既存の記述は変更しない
  - 対象ファイル：`docs/specifications.md`
  - 完了条件：
    - 3 章の表に `F-6` の行がある
    - 8 章に、削除ボタン・最低 1 行・インデックス振り直しの 3 点が書かれている
    - 既存の F-1〜F-5、V-1〜V-4 の記述が変更されていない（`git diff` で追加行のみである）
- [x] **T2. 初期表示の各行に「削除」ボタンが出力される（TDD）**
  - 依頼事項：`ShiftControllerTest` に、`[F-6] Given: GET / で初期表示するとき, When: 画面を取得すると, Then: 入力行（4 行）と同数の「削除」ボタン（`class="delete-row-btn"`）が含まれる` というテストを先に書き、RED を確認する。その後 `src/main/resources/templates/index.html` にヘッダの空 `<th>` と各行末尾の削除ボタン用 `<td>` を追加して GREEN にする。POST 後の再表示（入力行がエラー表示付きで再描画される場合）でも、行数分の削除ボタンが出力されることを 2 つ目のテストで確認する
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`、`src/main/resources/templates/index.html`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `@DisplayName` の先頭に `[F-6]` が付いた Given-When-Then のテストが存在する
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
- [x] **T3. JavaScript で行の削除・インデックス振り直し・最低 1 行維持を実装する**
  - 依頼事項：`src/main/resources/static/js/shift-form.js` を次のとおり変更する。(1) 既存の「行を追加」処理で作る行にも削除ボタン（`class="delete-row-btn"`、`type="button"`、文言「削除」）のセルを追加する。(2) `employee-rows` に `click` のイベント委譲を設定し、`delete-row-btn` がクリックされたらその行（`closest("tr")`）を削除する。(3) 削除後に、全行の `input` / `select` の `name` 属性を上から 0 始まりの連番に振り直す関数を呼ぶ。(4) 行数が 1 のときは削除ボタンを `disabled` にし、2 以上のときは有効にする関数を、初期表示時・行追加後・行削除後に呼ぶ。ファイル先頭のコメントも「追加・削除」に更新する
  - 対象ファイル：`src/main/resources/static/js/shift-form.js`
  - 完了条件：
    - `./mvnw spring-boot:run` で起動し、ブラウザで次を確認して実行ログに結果を書いた：(a) 初期 4 行から 2 行目を削除し、開発者ツールで残った行の `name` が `employees[0]`〜`employees[2]` の連番になっている (b) 「行を追加」後の新しい行が `employees[3]` になる (c) 1 行になるまで削除すると、残った行の削除ボタンが `disabled` になる (d) 削除・追加を繰り返した後に「シフトを作成」を送信して結果が表示される
    - 上記の確認ができない場合は、確認できなかった項目と理由を実行ログに書く（できたと偽らない）
    - `./mvnw test` が成功する
- [x] **T4. 全テストと静的解析を確認する**
  - 依頼事項：`./mvnw spotless:apply` で整形してから `./mvnw test` を実行し、Spotless・Checkstyle を含めて成功することを確認する。失敗した場合は原因となった Todo に戻って修正する
  - 対象ファイル：（変更なし。整形による差分が出た場合のみコミットに含める）
  - 完了条件：
    - `./mvnw test` が成功する（テスト件数と失敗 0 件を実行ログに書く）
    - Checkstyle の違反が 0 件である

## 実行ログ

- T3 試行 1/4：未完了 — ブラウザでの対話的テストが実行できない環境制限により、完了条件 (a)〜(d) の確認ができなかった。ただし、実装の正確性を以下で検証した：
  - JavaScript 構文チェック：`node -c shift-form.js` で成功
  - HTML 構造：`curl http://localhost:8080/` で 4 個の delete-row-btn を確認
  - コード実装：renumberInputIndices()、updateDeleteButtonState()、イベント委譲による削除処理がすべて実装されている
  - 次のステップ：実際のブラウザで以下を確認する必要がある：
    1. 初期 4 行から 2 行目を削除し、残った行の name 属性が employees[0]～employees[2] になること
    2. 「行を追加」後の新行が employees[3] になること
    3. 1 行のみになったとき、削除ボタンが disabled になること
    4. 削除・追加を繰り返した後、「シフトを作成」で結果が表示されること

- T3 手動確認（メインエージェントがヘッドレス Chrome で実施）：(a) 4 行から 2 行目を削除すると `employees[0]`〜`[2]` の連番、(b) 行追加で `employees[3]`、(c) 1 行になると削除ボタンが `disabled`、(d) 1 行から追加すると連番（0・1）で `disabled` が解除、をすべて確認した。実際の送信（「シフトを作成」）は未確認。
- T4：`./mvnw test` で 41 件成功・失敗 0・BUILD SUCCESS（Spotless・Checkstyle 含む）。

- Codex レビュー 1 回目の対応：MUST（言い換えコメント）は削除。SHOULD（POST で 4 行に補充し削除行が復活）は TDD で補充ループを削除して修正（実アプリで 6 行送信・1 行送信を確認）。SHOULD（JS の自動テスト）は、DOM テスト基盤の導入に新規依存が必要でユーザー承認が要るため今回は対応せず、ヘッドレス Chrome での手動確認を記録した。
- Codex レビュー 2 回目の対応：MUST なし。SHOULD（パラメータなし POST で入力表が 0 行になる）は TDD で修正（0 件のときだけ空行を 1 件補う）。SHOULD（JS の自動テスト）は新規依存が必要でユーザー承認が要るため対応せず。実アプリで 6 行・1 行の送信は確認済み。
