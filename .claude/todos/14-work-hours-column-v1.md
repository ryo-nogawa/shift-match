# Todo: 割当結果の表に「勤務時間」列を追加する

- Issue: #14
- ブランチ: feature/14-work-hours-column
- 版: v1
- 対象仕様: F-4, 7 章 出力仕様
- 作成日: 2026-09-24

## 前提

- 読むべきドキュメント・規約：`docs/specifications.md`（2 章の枠表、7 章 出力仕様）、`.claude/rules/tdd.md`、`.agents/rules/` のうち `test.md`・`formatting.md` など作業に関係するもの
- 既存のテスト（`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java` の `[T-1][F-4]` 休憩時刻のテスト付近）の書き方（`@WebMvcTest` + `MockMvc`、`@MockitoBean`、本文検証は `getContentAsString()` + `Assertions.assertTrue`。Hamcrest は使わない）に合わせる
- 設計方針：
  - `docs/` は変更しない（メインエージェントが更新済み）。サーバー側（Controller・Service・domain）は変更しない
  - 表示は `src/main/resources/templates/index.html` の割当結果の表のみ変更する。列順は「枠・氏名・勤務時間・休憩」
  - 勤務時間は枠から決まる固定値。既存の枠表示（`stat.index < 2 ? '早番' : '遅番'`）と同じ判定で、早番は `8:00〜17:00`、遅番は `12:00〜21:00`（先頭 0 なし、区切りは全角の `〜`）を出力する
  - 新しい依存ライブラリは追加しない。`pom.xml` は変更しない

## Todo

- [x] **T1. 割当結果の表ヘッダに「勤務時間」列が「氏名」と「休憩」の間に表示される（TDD）**
  - 依頼事項：`ShiftControllerTest` に `[F-4]` を付けた Given-When-Then のテストを先に書き、RED を確認する。内容は「有効な割当が存在するとき POST /shift を実行すると、割当結果の表ヘッダが `枠`・`氏名`・`勤務時間`・`休憩` の順で出力される」。その後 `index.html` の `<th>` に `勤務時間` を追加して GREEN にする（この時点では各行に `<td>` を足すと列数がずれるため、T1 では `<td>` の追加も同時に行い空セルでよい）
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`、`src/main/resources/templates/index.html`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `<th>` が `枠`・`氏名`・`勤務時間`・`休憩` の順になっていることを検証するテストが存在し成功する
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
- [ ] **T2. 早番の行に 8:00〜17:00、遅番の行に 12:00〜21:00 が表示される（TDD）**
  - 依頼事項：`ShiftControllerTest` に `[F-4]` のテストを先に書き、RED を確認する。早番 2 名・遅番 2 名の割当をモックし、結果表の 1〜2 行目（早番）の勤務時間セルが `8:00〜17:00`、3〜4 行目（遅番）が `12:00〜21:00` であることを検証する（行ごとに枠・氏名・勤務時間が同じ `<tr>` 内にあることを確認する）。その後 `index.html` の各行の勤務時間 `<td>` に、枠に応じた文字列を出力して GREEN にする
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`、`src/main/resources/templates/index.html`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - 早番 2 行に `8:00〜17:00`、遅番 2 行に `12:00〜21:00` が出力されることを検証するテストが存在し成功する
    - 既存の休憩時刻のテスト（`[T-1][F-4]`）が変更なしで成功する
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
- [ ] **T3. 全テストと静的解析が成功する**
  - 依頼事項：`./mvnw spotless:apply` で整形してから `./mvnw test` を実行し、失敗があれば修正する
  - 対象ファイル：T1・T2 で変更したファイル
  - 完了条件：
    - `./mvnw test` で全テストが成功する（Spotless・Checkstyle の違反 0 件）

## 実行ログ

<!-- implementer が試行結果（失敗理由・リトライ回数）を追記する欄。作成時は空のままにする -->
