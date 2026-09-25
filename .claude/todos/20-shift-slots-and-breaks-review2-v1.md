# Todo: Codex レビュー 2 ラウンド目の指摘対応

- Issue: #20
- ブランチ: feature/20-shift-slots-and-breaks
- 版: v1
- 対象仕様: F-1, F-4, V-2, 仕様 4 章・7 章・8 章
- 作成日: 2026-09-25

## 前提

- 読むべきもの：`target/reviews/code-quality-review.md`、`AGENTS.md`、`docs/specifications.md`（4.2・7・8 章）、`.claude/rules/tdd.md`、`.agents/rules/`（特に `test.md`・`comment.md`）
- `ShiftController`・`index.html`・`ShiftControllerTest` の現状を確認してから始める
- **`ShiftAssignmentServiceImpl`（割り当てアルゴリズム）には触れない。** レビューの MUST 3 件目（動的計画法と仕様の「総当たり」の食い違い）は、ユーザーの判断待ちのため、この Todo の対象外
- 各 Todo は、完了条件の「テストで検証している」項目を、実際にテストとして書いてからチェックを付ける（RED を先に確認する。既に GREEN になる場合は理由を実行ログに書く）。実行ログに、完了条件ごとの根拠（テストメソッド名）とテスト件数を書く
- Q1〜Q3 の全件を、この 1 回の依頼の中でやり切る。途中で止まる場合は【未完了】とし、未達の Todo と条件を報告する（【部分完了】はない）
- 各 Todo の終了時に `./mvnw spotless:apply` → `./mvnw test` が成功する状態でコミットする（Conventional Commits・日本語・仕様書の ID を付ける。末尾に `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>`）
- 新しい依存ライブラリは追加しない

## Todo

- [ ] **Q1. POST の全戻り経路で `slotLabels` をモデルに設定する（F-1、レビュー MUST）**
  - 依頼事項：`ShiftController` は、`GET /` では `slotLabels` を設定しているが、`POST /shift` のどの戻り経路（成立・不成立・V-2/V-3/V-5 のエラー）でも設定しておらず、`index.html` の 6 列の見出し・`select`・`data-slot-labels` が出力されない。枠ラベルの生成を 1 か所（`@ModelAttribute("slotLabels")` のメソッドなど）にまとめ、GET と POST の全経路で必ずモデルに入るようにする。`ShiftSlot.values()` から作る仕組みは維持する
  - 対象ファイル：`src/main/java/com/example/shiftmatch/controller/ShiftController.java`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `[F-1]` 成立（`assign` が結果を返す）・不成立（`Optional.empty()`）・V-2 エラー・V-3 エラー・V-5 エラーの 5 通りの `POST /shift` で、レスポンスの HTML に、入力表の見出しの 6 つの勤務時間（`07:30〜14:30`、`08:00〜15:30`、`08:30〜16:30`、`09:00〜16:30`、`09:00〜18:00`、`09:00〜18:30`）と、`data-slot-labels` 属性が含まれることをテストで検証している
    - `[F-1]` 上記のうち少なくとも成立と V-3 エラーの POST で、送信した各行に `wishes[0]`〜`wishes[5]` の `select` が 6 個ずつ存在することをテストで検証している
    - `./mvnw test` が成功する

- [ ] **Q2. 重複エラー（V-2）の行番号を、元のフォーム行のまま表示する（V-2、レビュー MUST）**
  - 依頼事項：`ShiftController` は、空行を除いた（V-1）リストを `findDuplicateNames` に渡しているため、空行があると、表示される行番号が元のフォーム行からずれる。`findDuplicateNames` には、空行を含む元の行順のリスト（`convertToEmployees(shiftForm)` の結果全体）を渡す（サービス側は空行を除外しつつ、元のインデックスを保持する実装になっている。`ShiftAssignmentService` の Javadoc を確認すること）。`assign` へ渡すリストも、サービスが空行を除外するため、元のリストでよいかを確認する（V-5 の人数判定には、空行を除いた人数を使う既存の実装を維持する）
  - 対象ファイル：`src/main/java/com/example/shiftmatch/controller/ShiftController.java`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `[V-2]` `@WebMvcTest` のため `ShiftAssignmentService` はモックだが、このテストでは `findDuplicateNames` に **実際の `ShiftAssignmentServiceImpl` の挙動** が必要になる。`when(service.findDuplicateNames(any())).thenAnswer(...)` の中で `new ShiftAssignmentServiceImpl().findDuplicateNames(引数)` を呼ぶ形にして、実サービスの結果を返す
    - `[V-2]` 1 行目が空（氏名なし）、2・3 行目が同名（`A`）の POST で、画面に「2, 3 行目」が表示され、「1, 2 行目」ではないことをテストで検証している
    - `[V-2]` 空行が間に入る場合（1 行目 `A`、2 行目が空、3 行目 `A`）に、「1, 3 行目」が表示されることをテストで検証している
    - `[V-1]` 空行だけの行があっても、V-3 のエラーが出ない（既存テストを維持する）
    - `./mvnw test` が成功する

- [ ] **Q3. 結果表のテストの期待値を仕様どおりに設定する（F-4、レビュー SHOULD）**
  - 依頼事項：`ShiftControllerTest` の、8 行の氏名・勤務時間・休憩時間を検証するテスト（`expectedWorkTimes` と `expectedBreakTimes` が空配列になっているもの）を直す。期待値は、仕様書 2 章の表のとおり：枠 1（2 名）`07:30〜14:30`＋`12:00〜12:45`、`07:30〜14:30`＋`12:00〜12:45`／枠 2 `08:00〜15:30`＋`12:45〜13:30`／枠 3 `08:30〜16:30`＋`12:45〜13:30`／枠 4 `09:00〜16:30`＋`13:30〜14:15`／枠 5 `09:00〜18:00`＋`13:30〜14:30`／枠 6（2 名）`09:00〜18:30`＋`14:15〜15:15`、`09:00〜18:30`＋`14:30〜15:30`
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - 期待値の配列が 8 件ずつ設定されている（`assertEquals(8, expectedWorkTimes.length)` のような確認、または配列リテラルが 8 件であること）
    - 結果表の `tbody` の行数がちょうど 8 であることを検証している
    - 各行が、枠 1 → 6 の順に、氏名・勤務時間・休憩時間を表示することを、8 行すべてについて検証している
    - このテストが、結果の表の内容を壊す（例：一時的に勤務時間の出力を変える）と失敗することを、実際に確認した（確認方法と結果を実行ログに書き、確認後は必ず元に戻す）
    - `./mvnw test` が成功する（テスト件数が減っていないこと）

## 実行ログ

<!-- implementer が Todo ごとに「完了条件ごとの根拠（テストメソッド名）」・テスト件数を追記する欄。作成時は空のままにする -->
