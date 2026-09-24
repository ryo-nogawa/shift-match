# Todo: 休憩時刻を自動割り当てして結果に表示する（T-1）

- Issue: #10
- ブランチ: feature/10-break-time
- 版: v1
- 対象仕様: T-1（決定済み）, C-3, F-3, F-4
- 作成日: 2026-09-24

## 前提

- 読むべきドキュメント・規約：
  - `docs/specifications.md`（2 章の休憩の割り当て表、7 章 出力仕様）
  - `.claude/rules/tdd.md`（Red → Green → Refactor を厳守）
  - `.agents/rules/test.md`（`@Nested` ＋ `@DisplayName`（Given-When-Then）、仕様 ID を `[T-1]` 形式で先頭に付与、AssertJ・Hamcrest 禁止、JUnit5 標準 Assertions のみ）
  - `.agents/rules/naming.md`、`.agents/rules/javadoc.md`（public/package-private に Javadoc 必須）、`.agents/rules/lambda.md`（メソッド参照禁止）、`.agents/rules/formatting.md`（`./mvnw spotless:apply`）
- 休憩の仕様（決定済み）：1 人 1 時間・1 時間刻み・1 人ずつ順番。成立案の 4 名に次のとおり割り当てる

  | 対象 | 休憩時刻 |
  | --- | --- |
  | 早番 1 人目（`earlyEmployees` の 0 番目） | 13:00〜14:00 |
  | 早番 2 人目（1 番目） | 14:00〜15:00 |
  | 遅番 1 人目（`lateEmployees` の 0 番目） | 15:00〜16:00 |
  | 遅番 2 人目（1 番目） | 16:00〜17:00 |

- 設計方針：
  - **`AssignmentResult` のコンストラクタ（コンポーネント）は変更しない**。既存テスト・`ShiftAssignmentServiceImpl` を壊さないため、休憩は `AssignmentResult` から導出する。
  - `com.example.shiftmatch.domain.BreakTime`（`record BreakTime(Employee employee, LocalTime start, LocalTime end)`）を新設し、`AssignmentResult#breakTimes()` が `List<BreakTime>` を「早番 → 遅番、各組は `earlyEmployees`／`lateEmployees` の並び順」で返す。
  - 同名の従業員が存在しない前提（V-2 で排除済み）だが、`Employee` の同値判定に依存せず、リストの位置（インデックス）から休憩を決める。
  - 同点ルール・スコア・不成立判定・`ShiftAssignmentServiceImpl` のアルゴリズムは**変更しない**。
  - 新しい依存は追加しない。`pom.xml` は変更しない。
  - 画面（`src/main/resources/templates/index.html`）の割当結果の表は現在「枠｜氏名」の 2 列。これに「休憩」列（例：`13:00〜14:00`）を追加する。表示文字列は `HH:mm〜HH:mm`。
  - Controller は現状 `assignmentResult` をモデルに載せているだけなので、テンプレートから `assignmentResult.breakTimes()` を参照する形でよい（Controller の変更は不要な想定）。
  - `ShiftControllerTest` の既存 F-4 テスト（`AssignmentResult` を直接生成している）は、休憩列の追加後も成功するように保つ。

## Todo

- [x] **T1. [T-1] 早番 2 名の休憩時刻（13:00・14:00 開始）を `AssignmentResult#breakTimes()` で導出する**
  - 依頼事項：
    - `src/test/java/com/example/shiftmatch/domain/AssignmentResultTest.java` を新規作成し、先にテストを書く（RED 確認）。早番 2 名・遅番 2 名の `AssignmentResult` を作り、`breakTimes()` の先頭 2 件が 早番 1 人目＝13:00〜14:00、早番 2 人目＝14:00〜15:00 であることを検証する
    - `BreakTime` レコードを新設し、`AssignmentResult#breakTimes()` を最小実装で追加する（この時点では早番分のみ実装してよい）
    - `@DisplayName` の先頭に `[T-1]` を付ける
  - 対象ファイル：`src/main/java/com/example/shiftmatch/domain/BreakTime.java`、`src/main/java/com/example/shiftmatch/domain/AssignmentResult.java`、`src/test/java/com/example/shiftmatch/domain/AssignmentResultTest.java`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `./mvnw test -Dtest=AssignmentResultTest` が成功する
    - `BreakTime`・`breakTimes()` に Javadoc がある
- [x] **T2. [T-1] 遅番 2 名の休憩時刻（15:00・16:00 開始）を追加する**
  - 依頼事項：
    - `AssignmentResultTest` に、`breakTimes()` の全体が 4 件で、順序が 早番 1 人目 13:00〜14:00 → 早番 2 人目 14:00〜15:00 → 遅番 1 人目 15:00〜16:00 → 遅番 2 人目 16:00〜17:00 であることを検証するテストを先に書く（RED 確認）
    - 各休憩が重ならず 1 時間であることを検証するテストも追加する（`end` が次の `start` と一致）
    - `breakTimes()` を遅番分まで実装する
  - 対象ファイル：`src/main/java/com/example/shiftmatch/domain/AssignmentResult.java`、`src/test/java/com/example/shiftmatch/domain/AssignmentResultTest.java`
  - 完了条件：
    - RED を確認してから実装した
    - `./mvnw test -Dtest=AssignmentResultTest` が成功する
- [x] **T3. [T-1, F-4] 結果の表に休憩列を表示する**
  - 依頼事項：
    - `ShiftControllerTest` の F-4 の結果表示テストの近くに、`[T-1]` `[F-4]` のテストを先に追加する。`AssignmentResult`（早番 2 名・遅番 2 名）を返すようモックし、POST `/shift` のレスポンス本文（`getContentAsString()` ＋ `assertTrue(body.contains(...))`）に `13:00〜14:00`、`14:00〜15:00`、`15:00〜16:00`、`16:00〜17:00` が含まれることを検証する（RED 確認）
    - `index.html` の結果表を「枠｜氏名｜休憩」の 3 列にし、`assignmentResult.breakTimes()` の各要素（`employee().name()`、`start`、`end`）を早番 → 遅番の順で表示する。時刻の書式は `HH:mm`（`th:text` と `#temporals.format` またはタイムゾーン非依存の `LocalTime#toString`／`String.format` を使い、実行環境のロケールに依存しないこと）
    - 枠の列（早番／遅番）は、`breakTimes()` の先頭 2 件が早番、後ろ 2 件が遅番であることに基づいて出し分ける（`assignmentResult.earlyEmployees()`／`lateEmployees()` の走査に `stat.index` で対応する休憩を引く形でもよい）
  - 対象ファイル：`src/main/resources/templates/index.html`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - RED を確認してから実装した
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する（既存テストを含む）
    - 結果表に休憩列が存在し、4 つの休憩時刻が画面本文に含まれる
- [ ] **T4. 全体テストと静的解析を確認する**
  - 依頼事項：`./mvnw spotless:apply` を実行後、`./mvnw test` を全体で実行し、結果を実行ログに記録する
  - 対象ファイル：（なし。必要なら整形のみ）
  - 完了条件：
    - `./mvnw test` で全テストが成功する（Spotless・Checkstyle の違反 0 件）
    - `docs/specifications.md` の 2 章・7 章の記述と、実装の休憩時刻（13:00〜14:00 / 14:00〜15:00 / 15:00〜16:00 / 16:00〜17:00）が一致している

## 実行ログ

<!-- implementer が試行結果（失敗理由・リトライ回数）を追記する欄。作成時は空のままにする -->
