# Todo: Codex レビュー 1 回目の指摘対応（休憩時刻 T-1）

- Issue: #10
- ブランチ: feature/10-break-time
- 版: v2（レビュー指摘対応。v1 は全件完了済み）
- 対象仕様: T-1, F-4, H-1, H-2
- 作成日: 2026-09-24

## 前提

- 読むべきドキュメント・規約：`docs/specifications.md`（2 章・7 章）、`.claude/rules/tdd.md`、`.agents/rules/test.md`、`.agents/rules/comment.md`、`.agents/rules/exception.md`、`.agents/rules/javadoc.md`、`.agents/rules/lambda.md`
- 元の指摘：`target/reviews/code-quality-review.md`（MUST 2 件・SHOULD 1 件）
- 実装上の注意：
  - `AssignmentResult` のコンパクトコンストラクタの不変条件は、`earlyEmployees` と `lateEmployees` が**それぞれちょうど 2 件**（H-1）でなければ `IllegalArgumentException` を投げる。`List.copyOf` の前後どちらでもよい（null は従来どおり NPE のままでよい）。検証は if 文で行い、例外を try-catch で捕捉しない（`exception.md`）
  - 既存テスト・`ShiftAssignmentServiceImpl` は早番 2・遅番 2 で生成しているため影響しない想定。`ShiftControllerTest` など他のテストで 2 件以外の `AssignmentResult` を生成している箇所があれば、仕様（各 2 名）に合わせてテスト側を直す
  - 休憩の表示区切りは仕様書どおり全角波ダッシュ **`〜`（U+301C）**。現状の実装は `～`（U+FF5E）になっている

## Todo

- [ ] **T1. [H-1] `AssignmentResult` が早番・遅番それぞれ 2 件でない場合に例外を投げる**
  - 依頼事項：
    - `AssignmentResultTest` に `@Nested` の異常系グループ（`@DisplayName` は Given-When-Then、先頭に `[H-1]`）を先に追加する（RED 確認）：早番 1 件、早番 3 件、遅番 1 件、遅番 3 件の各ケースで `assertThrows(IllegalArgumentException.class, ...)` になること
    - コンパクトコンストラクタに件数チェックを最小実装で追加する。Javadoc に例外条件を追記する
    - `AssignmentResult` に載せた `breakTimes()` 内の「何をしているか」を言い換えただけのコメント（`// 早番の休憩（…）`・`// 遅番の休憩（…）`）を削除する（`comment.md`）
  - 対象ファイル：`src/main/java/com/example/shiftmatch/domain/AssignmentResult.java`、`src/test/java/com/example/shiftmatch/domain/AssignmentResultTest.java`
  - 完了条件：
    - RED を確認してから実装した
    - `./mvnw test -Dtest=AssignmentResultTest` が成功する
    - `AssignmentResult.java` に「何をしているか」の言い換えコメントが残っていない
- [ ] **T2. [T-1, F-4] 結果表の休憩表示を仕様書どおり `HH:mm〜HH:mm`（U+301C）にし、行ごとに検証する**
  - 依頼事項：
    - `ShiftControllerTest` の休憩表示テストを先に強化する（RED 確認）：本文に `13:00〜14:00`、`14:00〜15:00`、`15:00〜16:00`、`16:00〜17:00`（区切りは U+301C `〜`）が含まれること、および `早番` → 氏名 → `13:00〜14:00` のように、各行の枠・氏名・休憩範囲が同じ `<tr>` 内で対応していることを、正規表現（`Pattern`／`String#matches`、DOTALL）で 4 行ぶん検証する。行の並びが早番 2 行 → 遅番 2 行の順であることも検証する
    - `index.html` の休憩表示を `〜`（U+301C）に修正する
  - 対象ファイル：`src/main/resources/templates/index.html`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - RED（現状の `～`（U+FF5E）で失敗）を確認してから実装した
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
    - `grep -rn "～" src/main src/test` で U+FF5E が休憩表示に残っていない
- [ ] **T3. 全体テストと静的解析を確認する**
  - 依頼事項：`./mvnw spotless:apply` の後、`./mvnw test` を全体で実行し、実行ログに結果を追記する
  - 対象ファイル：（整形のみ）
  - 完了条件：
    - `./mvnw test` で全テストが成功する（Spotless・Checkstyle 違反 0 件）

## 実行ログ

<!-- implementer が試行結果を追記する欄 -->
