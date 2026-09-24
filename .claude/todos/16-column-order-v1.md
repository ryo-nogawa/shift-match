# Todo: 割当結果の表の列順を「氏名・勤務時間・休憩」にする

- Issue: #16
- ブランチ: feature/16-working-hours-display
- 版: v1
- 対象仕様: F-4, 7 章 出力仕様
- 作成日: 2026-09-24

## 前提

- 読むべきドキュメント・規約：`docs/specifications.md`（7 章 出力仕様）、`.claude/rules/tdd.md`、`.agents/rules/test.md`・`formatting.md`・`comment.md`
- 現状：割当結果の表は「勤務時間・氏名・休憩」の順（`src/main/resources/templates/index.html`）。変更後は **「氏名・勤務時間・休憩」** の順にする
- 設計方針：
  - `docs/` は変更しない（メインエージェントが更新済み）。サーバー側は変更しない。新しい依存は追加しない
  - 列見出し・セルの内容は変えない（見出しは `氏名`・`勤務時間`・`休憩` のまま。勤務時間は `08:00〜17:00` / `12:00〜21:00`、休憩の表示も現状どおり）。入れ替えるのは列の並びだけ
  - 変更するのは `index.html` の割当結果の表と `ShiftControllerTest`（`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`）のみ
  - テストの書き方は既存に合わせる（`@WebMvcTest` + `MockMvc`、`Assertions.assertTrue`。Hamcrest は使わない）
  - 既存の `[F-4]` 表ヘッダの列順テスト（DisplayName に「勤務時間」・「氏名」・「休憩」の順とあるもの、`namePos`・`workHoursPos`・`breakPos` で位置を比較している）が対象。期待する順序を新仕様に更新する

## Todo

- [x] **T1. 割当結果の表が「氏名」・「勤務時間」・「休憩」の順で出力される（TDD）**
  - 依頼事項：`ShiftControllerTest` の `[F-4]` 表ヘッダの列順テストを、「氏名」→「勤務時間」→「休憩」の順を期待する内容に書き換え（DisplayName・エラーメッセージ文言も合わせる）、先にテストだけ変更して RED（アサーション失敗）を確認する。あわせて、各データ行の中でも氏名が勤務時間より前に出力されること（同じ `<tr>` 内で氏名の位置 < 勤務時間の位置）を検証するテストを追加する。その後 `index.html` の `<th>` と各行の `<td>` を「氏名」・「勤務時間」・「休憩」の順に入れ替えて GREEN にする
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`、`src/main/resources/templates/index.html`
  - 完了条件：
    - テストを先に変更し、アサーションで失敗（RED）することを確認した
    - 表ヘッダが `氏名`・`勤務時間`・`休憩` の順であることを検証するテストが存在し成功する
    - データ行で氏名が勤務時間より前に出力されることを検証するテストが存在し成功する
    - 勤務時間（`08:00〜17:00` / `12:00〜21:00`）と休憩時刻の期待値は変更されていない
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
- [x] **T2. 全テストと静的解析が成功する**
  - 依頼事項：`./mvnw spotless:apply` で整形してから `./mvnw test` を実行し、失敗があれば修正する
  - 対象ファイル：T1 で変更したファイル
  - 完了条件：
    - `./mvnw test` で全テストが成功する（Spotless・Checkstyle の違反 0 件）

## 実行ログ

<!-- implementer が試行結果（失敗理由・リトライ回数）を追記する欄。作成時は空のままにする -->
