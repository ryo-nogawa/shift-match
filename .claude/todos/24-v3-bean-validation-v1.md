# Todo: V-3 の開始・終了時間バリデーションを Bean Validation に移行する

- Issue: #24
- ブランチ: refactor/24-v3-bean-validation
- 版: v1
- 対象仕様: V-3（V-1 の除外規則・V-1 → V-5 の実行順序は維持）
- 作成日: 2026-09-26

## 前提

- 作業ディレクトリは worktree `/Users/ryonogawa/Develop/java/projects/shift-match-24`。すべてのコマンドはここで実行する（メインの `shift-match` ディレクトリは触らない）
- 読むべきドキュメント・規約：`docs/specifications.md` 4 章の V-x 表（110〜116 行目）、`.claude/rules/tdd.md`、`.agents/rules/test.md`、`.agents/rules/javadoc.md`、`.agents/rules/comment.md`、`.agents/rules/naming.md`
- **リファクタリングであり、振る舞いは変えない。** 現在のコントローラー `ShiftController#validateTimeRanges` / `validateTimeOption` の挙動が正。既存の V-3 テスト（`ShiftControllerTest` の `[V-3][V-1]` 群）は **一切書き換えずに** 通り続けること
- `pom.xml` には `spring-boot-starter-validation` が既にある。**新しい依存は追加しない**
- 現行のエラー条件とメッセージ（一字一句変えない）。対象は「氏名が入力されていて、`off` でない行」だけ（V-1）
  - 開始が `null` または空 → `開始が未選択です`
  - 終了が `null` または空 → `終了が未選択です`
  - 開始が選択肢（07:30〜18:30 の 30 分刻み、`HH:mm`）にない → `開始は選択肢にありません`
  - 終了が選択肢にない → `終了は選択肢にありません`
  - 開始・終了がともに選択肢内で、開始 ≧ 終了（`String#compareTo` の結果が 0 以上）→ `開始は終了より前にしてください`
- 現行の表示：コントローラーはエラーを `InvalidTimeRangeError(int rowIndex, String message)`（`domain` パッケージ）のリストにして、モデル属性 `timeRangeErrors` に入れ、`index.html` がそれを表示している。**この属性名・型・`index.html` は変更しない**
- 1 行内のエラーの並びは「開始のエラー → 終了のエラー」。「開始は終了より前に…」は開始・終了がともに正常なときだけ出るので、同じ行に 3 件同時に出ることはない
- Bean Validation の違反の集まり（`Set`）には順序の保証がない。`BindingResult` から `InvalidTimeRangeError` を作るときは **行番号の昇順、同じ行では開始 → 終了の順** に並べ替えること
- 設計方針（この構成で実装する）
  1. 選択肢一覧 `TIME_OPTIONS` を、コントローラーとバリデーターの両方から使えるよう `controller` パッケージの新クラス `TimeOptions`（`final` クラス、private コンストラクタ、`public static final List<String> VALUES`）へ移す
  2. `controller` パッケージに、クラスに付ける制約アノテーション `@ValidTimeRange` と、`ConstraintValidator<ValidTimeRange, EmployeeForm>` の実装 `ValidTimeRangeValidator` を作る
  3. `ValidTimeRangeValidator` は `context.disableDefaultConstraintViolation()` した上で、`buildConstraintViolationWithTemplate(メッセージ).addPropertyNode("start" または "end").addConstraintViolation()` で違反を追加する。開始 ≧ 終了の違反は `end` のノードに付ける（Spring が `employees[行番号].end` のフィールドエラーにしてくれるため、行番号を取り出せる）
  4. `EmployeeForm` に `@ValidTimeRange` を付け、`ShiftForm#employees` に `@Valid` を付ける。コントローラーの `createShift` は `@Valid @ModelAttribute("shiftForm") ShiftForm shiftForm, BindingResult bindingResult` を受け取り、`bindingResult.getFieldErrors()` から `employees[N].start` / `employees[N].end` を `InvalidTimeRangeError` に変換する
  5. 手書きの `validateTimeRanges` と `validateTimeOption` は削除する
- `BindingResult` を引数に取るので、検証エラーがあってもメソッドは実行される（今までどおり、重複・上限のエラーとまとめて画面に出る）。V-1 → V-5 の実行順序・「エラーがあれば算出しない」の分岐は変えない
- フィールドエラーの文字列は `FieldError#getDefaultMessage()` で取れる。メッセージはテンプレート文字列（`{...}` を含まない日本語の固定文）として渡すこと
- 型不一致など、V-3 以外の `bindingResult` のエラーは今回のスコープ外（無視してよい）

## Todo

- [ ] **T1. 時刻の選択肢を `TimeOptions` クラスへ切り出す（振る舞いは変えない）**
  - 依頼事項：`ShiftController` の `TIME_OPTIONS`・`createTimeOptions()` と、それが使う定数（`FIRST_TIME_OPTION`・`LAST_TIME_OPTION`・`TIME_OPTION_STEP_MINUTES`・`TIME_FORMATTER` のうち選択肢の生成に必要なもの）を、新クラス `TimeOptions`（`public final class`、`public static final List<String> VALUES`）へ移す。`ShiftController` は `TimeOptions.VALUES` を参照する。`@ModelAttribute("timeOptions")` の戻り値と、`parseTimeOrNull` が使う `TIME_FORMATTER` はコントローラーの動作を変えないように残す。これは既存テストで守られたリファクタリングなので、新しいテストは追加しない
  - 対象ファイル：`src/main/java/com/example/shiftmatch/controller/TimeOptions.java`（新規）、`src/main/java/com/example/shiftmatch/controller/ShiftController.java`
  - 完了条件：
    - `./mvnw test -Dtest=ShiftControllerTest` が成功し、テストファイルは 1 行も変更されていない（`git diff --stat -- src/test` が空）
    - `ShiftController` に `createTimeOptions` メソッドと `TIME_OPTIONS` 定数が残っていない
    - `TimeOptions.VALUES` が 07:30〜18:30 の 30 分刻み、23 件である（`ShiftControllerTest` の `timeOptions` の 23 件を確認するテストが成功することで確認する）
    - `TimeOptions` に Javadoc がある（`.agents/rules/javadoc.md` に従う）
    - コミットした（Conventional Commits、日本語。例：`refactor: 時刻の選択肢を TimeOptions クラスへ切り出す`）
- [ ] **T2. `@ValidTimeRange` と `ValidTimeRangeValidator` を TDD で作る**
  - 依頼事項：`ValidTimeRangeValidatorTest`（`src/test/java/com/example/shiftmatch/controller/`）を **先に** 書く。`jakarta.validation.Validation.buildDefaultValidatorFactory().getValidator()` で `EmployeeForm` を直接検証する単体テストにする。1 サイクルにつき 1 つの振る舞いを Red → Green → Refactor で進め、次の順に作る：(1) 開始が未選択（`null`・空文字）、(2) 終了が未選択、(3) 開始が選択肢にない（例：`07:00`、`09:15`、`abc`）、(4) 終了が選択肢にない、(5) 開始 = 終了、(6) 開始 > 終了（例：`10:00` と `09:00`）、(7) 氏名が空（`null`・空白のみ）の行は違反なし（V-1）、(8) 休み（`off = true`）の行は違反なし、(9) 正常な開始・終了（例：`09:00`〜`17:00`）は違反なし。各テストは違反の **メッセージと、違反が付いたプロパティ（`start` / `end`）** を検証する。テスト名に `[V-3]`（V-1 の除外は `[V-1]`）を含める。実装は、前提の「設計方針」の 2・3 のとおり `@ValidTimeRange`（`@Constraint(validatedBy = ValidTimeRangeValidator.class)`、`@Target(TYPE)`、`@Retention(RUNTIME)`、`String message() default ""`、`Class<?>[] groups()`、`Class<? extends Payload>[] payload()`）と `ValidTimeRangeValidator` を作る。この Todo では `EmployeeForm` に `@ValidTimeRange` を付けるところまで行う（コントローラーはまだ変更しない。コントローラー側は手書き検証が残っているので、`@Valid` を付けていない間は二重にはならない）
  - 対象ファイル：`src/main/java/com/example/shiftmatch/controller/ValidTimeRange.java`（新規）、`src/main/java/com/example/shiftmatch/controller/ValidTimeRangeValidator.java`（新規）、`src/main/java/com/example/shiftmatch/controller/EmployeeForm.java`、`src/test/java/com/example/shiftmatch/controller/ValidTimeRangeValidatorTest.java`（新規）
  - 完了条件：
    - 各テストについて、実装前に意図した理由（アサーションの失敗。コンパイルエラーだけで終わらせない）で RED になったことを実行ログに記録した
    - `./mvnw test -Dtest=ValidTimeRangeValidatorTest` が成功する
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する（コントローラー側は未変更のため）
    - メッセージが前提に書いた 5 種類と一字一句同じである（テストの期待値に直接書く）
    - `@DisplayName` の先頭に `[V-3]`（または `[V-1]`）が付き、Given-When-Then で書かれている（`.agents/rules/test.md` に従う）
    - 新しいクラスに Javadoc がある
    - TDD の 1 サイクルごと（または意味のある単位）でコミットした
- [ ] **T3. コントローラーを Bean Validation に切り替え、手書きの検証を削除する**
  - 依頼事項：`ShiftForm#employees` に `@Valid` を付ける。`ShiftController#createShift` の引数を `@Valid @ModelAttribute("shiftForm") ShiftForm shiftForm, BindingResult bindingResult, Model model` に変え、`bindingResult.getFieldErrors()` のうち `employees[N].start` / `employees[N].end` のものを `InvalidTimeRangeError(N, defaultMessage)` へ変換するプライベートメソッド（例：`toTimeRangeErrors(BindingResult)`）を作る。並び順は「行番号の昇順、同じ行では start → end」。`validateTimeRanges` と `validateTimeOption` を削除する。既存の `[V-3][V-1]` テストは書き換えない。加えて、次の 2 つを検証するテストを `ShiftControllerTest` の `[V-3][V-1]` のグループへ **先に** 追加して RED を確認してから実装する：(a) 複数行にエラーがあるとき、`timeRangeErrors` が行番号の昇順で並ぶ、(b) 同じ行の開始・終了がともに選択肢外のとき、開始のエラー → 終了のエラーの順に並ぶ。ただし現行の手書きロジックでもこれらは通るはずなので、追加テストが最初から GREEN になる場合は「振る舞い維持を固定する特性テスト」として扱い、RED の確認は既存テストを一時的に壊さずに、`@Valid` を付けて `toTimeRangeErrors` を空実装にした状態でこのテストと既存 V-3 テストが失敗（RED）することを確認する形にしてよい
  - 対象ファイル：`src/main/java/com/example/shiftmatch/controller/ShiftController.java`、`src/main/java/com/example/shiftmatch/controller/ShiftForm.java`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
    - 既存の `[V-3][V-1]` テストの期待値（メッセージ・行番号）が 1 文字も変更されていない（`git diff` で既存テストの削除行・変更行がないこと。追加のみ）
    - `ShiftController` に `validateTimeRanges` と `validateTimeOption` が存在しない（`grep -n "validateTimeRanges\|validateTimeOption" src/main/java` が 0 件）
    - `index.html` と `InvalidTimeRangeError` は変更されていない（`git diff --stat` に含まれない）
    - `createShift` の V-1 → V-5 の順序（氏名の重複 V-2 → 開始・終了 V-3 → 上限 V-5 を集約して、エラーがあれば `index` を返し `assign` を呼ばない）が変わっていない
    - 追加テストの `@DisplayName` の先頭に `[V-3]` が付き、Given-When-Then で書かれている
    - Javadoc が更新されている（削除したメソッドの記述が残っていない）
    - コミットした
- [ ] **T4. 全テストと静的解析を確認する**
  - 依頼事項：`./mvnw spotless:apply` で整形してから `./mvnw test` を実行し、Spotless・Checkstyle を含めて通ることを確認する。違反があれば直して再実行する。実行結果（テスト件数、Checkstyle 違反件数）を実行ログに記録する。未使用の import やコメントアウトされたコードが残っていないことも確認する
  - 対象ファイル：`src/main/java/com/example/shiftmatch/controller/` 配下、`src/test/java/com/example/shiftmatch/controller/` 配下
  - 完了条件：
    - `./mvnw test` が成功する（テスト 0 失敗、Spotless 違反 0、Checkstyle 違反 0）
    - `git status --short` が空である（未コミットの変更がない）
    - `@Disabled` やコメントアウトで回避したテストがない
    - 整形による変更があった場合はコミットした（例：`style: Spotless で整形する`）

## 実行ログ

<!-- implementer が試行結果（失敗理由・リトライ回数）を追記する欄。作成時は空のままにする -->
