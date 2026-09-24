# Todo: 割当結果の表の見出し「休憩」を「休憩時間」に変更する

- Issue: #16
- ブランチ: feature/16-working-hours-display
- 版: v1
- 対象仕様: F-4, 7 章 出力仕様
- 作成日: 2026-09-24

## 前提

- 読むべきドキュメント・規約：`docs/specifications.md`（7 章 出力仕様）、`.claude/rules/tdd.md`、`.agents/rules/test.md`・`formatting.md`・`comment.md`
- 現状：割当結果の表の見出しは `氏名`・`勤務時間`・`休憩`。変更後は **`氏名`・`勤務時間`・`休憩時間`**（3 つ目の見出しだけを「休憩時間」にする）
- 設計方針：
  - `docs/` は変更しない（メインエージェントが更新済み）。サーバー側は変更しない。新しい依存は追加しない
  - 列の並び、勤務時間（`08:00〜17:00` / `12:00〜21:00`）、休憩時刻のセルの内容は変えない。変えるのは見出し文字列だけ
  - 変更するのは `src/main/resources/templates/index.html` の割当結果の表の `<th>` と `ShiftControllerTest`（`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`）のみ
  - 注意：`indexOf("休憩")` や `contains("休憩")` は「休憩時間」にも一致するため、テストが変更前でも成功してしまう。RED にするため、見出しの検証は `休憩時間` の完全な文字列で行い、`<th>休憩</th>`（「時間」なし）が出力されないことも検証する

## Todo

- [ ] **T1. 割当結果の表ヘッダが「氏名」・「勤務時間」・「休憩時間」の順で出力される（TDD）**
  - 依頼事項：`ShiftControllerTest` の `[F-4]` 表ヘッダの列順テスト（`namePos`・`workHoursPos`・`breakPos` で位置を比較しているもの）を、見出しの `休憩` を `休憩時間` に変更する（DisplayName・エラーメッセージ文言・`indexOf` の引数も合わせる）。あわせて `<th>休憩</th>` が出力されないことを検証する。先にテストだけ変更して RED（アサーション失敗）を確認する。その後 `index.html` の `<th>休憩</th>` を `<th>休憩時間</th>` にして GREEN にする
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`、`src/main/resources/templates/index.html`
  - 完了条件：
    - テストを先に変更し、アサーションで失敗（RED）することを確認した
    - 表ヘッダが `氏名`・`勤務時間`・`休憩時間` の順であることを検証するテストが存在し成功する
    - `<th>休憩</th>` が出力されないことを検証するテストが存在し成功する
    - 勤務時間と休憩時刻の期待値は変更されていない
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
- [ ] **T2. 全テストと静的解析が成功する**
  - 依頼事項：`./mvnw spotless:apply` で整形してから `./mvnw test` を実行し、失敗があれば修正する
  - 対象ファイル：T1 で変更したファイル
  - 完了条件：
    - `./mvnw test` で全テストが成功する（Spotless・Checkstyle の違反 0 件）

## 実行ログ

<!-- implementer が試行結果（失敗理由・リトライ回数）を追記する欄。作成時は空のままにする -->
