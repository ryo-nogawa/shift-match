# Todo: 営業日ごとの月間シフト算出と不成立判定（月間 2/5）

- Issue: #43（親 Issue: #40）
- ブランチ: feature/43-monthly-shift
- 版: v1
- 対象仕様: F-3、F-5、F-11、V-1〜V-9、H-1〜H-3、4.1 節（希望の優先順位）、5 章、6 章、7.2 節（選定根拠ログ）
- 作成日: 2026-09-26

## 前提

- 読むべきドキュメント・規約：`docs/specifications.md` の 3 章（F-3・F-5・F-11）・4.1 節・4.2 節・5 章・6 章・7.2 節、`.claude/rules/tdd.md`、`.agents/rules/` の `test.md`・`naming.md`・`javadoc.md`・`comment.md`・`exception.md`・`lambda.md`・`thread-safety.md`・`formatting.md`・`checkstyle.md`
- 本 Issue の範囲は **ドメイン層とサービス層だけ**。`controller/`・`templates/`・`static/`・`persistence/` は変更しない（画面は #44、結果表示は #45、保存は #46 の対象）。既存の 1 日分の画面（`ShiftController`）はそのまま動かしておく
- **新しい依存ライブラリは追加しない**（`pom.xml` を変更しない）。メソッド参照（`Type::method`）は使わない（`lambda.md`）
- 既存コードとの関係
  - 1 日分の割り当ては既存の `ShiftAssignmentService.assign(List<Employee>)`（`service/ShiftAssignmentServiceImpl.java`）をそのまま使う。**アルゴリズム（DP・同点規則・不成立判定）は書き換えない。** 営業日ごとにその日の希望から `Employee` のリストを作り、`assign` を呼ぶ。`Employee` は `Employee.working(...)`／`Employee.onLeave(...)` で作れる
  - `assign` は「休みでない人が 8 名未満」のとき空の `Optional` を返す（V-4 の不成立）
  - 営業日は #42 の `HolidayService.businessDays(YearMonth)`（月〜金かつ祝日でない日、昇順）を使う。対象月の判定は `HolidayService.isSupported(YearMonth)`（V-8）
  - 例外・エラーの作り方は `domain/DuplicateNameError` など既存の `*Error` と `.agents/rules/exception.md` に倣う
  - 時刻の選択肢（7:30〜18:30 の 30 分単位）は `controller/TimeOptions` にある。`service` から `controller` への依存は避けたいので、必要なら判定ロジックを `domain/` に持つ（`controller` 側の既存コードは変更しない）
- 設計（この名前で作る。すべて `com.example.shiftmatch` 配下）
  - `domain/DailyWish`：`record DailyWish(boolean off, LocalTime start, LocalTime end)`（1 日分の希望。休みのとき start・end は null）
  - `domain/EmployeeProfile`：`record EmployeeProfile(String name, EmploymentType employmentType, Map<DayOfWeek, DailyWish> baseShifts)`（月〜金の基本シフト）
  - `domain/ShiftAdjustment`：`record ShiftAdjustment(LocalDate date, String employeeName, DailyWish wish)`（日ごとの個別変更）
  - `domain/MonthlyShiftInput`：`record MonthlyShiftInput(YearMonth month, List<EmployeeProfile> employees, List<ShiftAdjustment> adjustments)`
  - `domain/DailyShiftResult`：`record DailyShiftResult(LocalDate date, int availableCount, Optional<AssignmentResult> assignment)`（不成立の日は `assignment` が空。`availableCount` は勤務できる人数＝有効な従業員のうち休みでない人の数）
  - `domain/MonthlyShiftResult`：`record MonthlyShiftResult(YearMonth month, List<DailyShiftResult> days)`（営業日順）。成立日数・不成立日数を返すメソッドは作らない（#45 の範囲）
  - `domain/InputError`：`record InputError(String code, String message)`（`code` は `V-2` のような仕様 ID）と、`domain/InvalidMonthlyInputException`（`List<InputError> errors()` を持つ実行時例外）
  - `service/MonthlyInputValidator`：`List<InputError> validate(MonthlyShiftInput input)`（V-1〜V-9 を **この順** に検証し、見つかったエラーを V の番号順にまとめて返す）
  - `service/MonthlyShiftService`（インタフェース）と `MonthlyShiftServiceImpl`：`MonthlyShiftResult create(MonthlyShiftInput input)`（検証エラーがあれば `InvalidMonthlyInputException`）
  - `service/SelectionRationaleLogger`：営業日ごとの選定根拠ログ
- テストの注意：`HolidayService` はテスト内のスタブ（または Mockito）で差し替え、ネットワーク・DB に依存させない。テストの `@DisplayName` の先頭に仕様 ID（例：`[V-4]`）を付け、Given-When-Then で書く
- 整形：コミット前に `./mvnw spotless:apply` を実行する。コミットメッセージは Conventional Commits 形式の日本語（例：`feat: [F-11] 個別変更を優先して日ごとの希望を決める`）。コミットは Todo 1 件ごと

## Todo

- [x] **T1. 月間入力・結果のドメインモデルを作る（F-3、F-11）**
  - 依頼事項：「前提 → 設計」のうち `DailyWish`・`EmployeeProfile`・`ShiftAdjustment`・`MonthlyShiftInput`・`DailyShiftResult`・`MonthlyShiftResult`・`InputError`・`InvalidMonthlyInputException` を `domain/` に作る。コレクションを受け取る record は、コンパクトコンストラクタで `List.copyOf`／`Map.copyOf` により不変にする（`AssignmentResult` を参考）。あわせて `DailyWish` から `Employee` を作るメソッド（例：`EmployeeProfile` と `DailyWish` から `Employee` を返す `toEmployee`）を用意する。休みなら `Employee.onLeave(name, type)`、それ以外は `Employee.working(name, type, start, end)`
  - 対象ファイル：`src/main/java/com/example/shiftmatch/domain/` の上記ファイル、`src/test/java/com/example/shiftmatch/domain/` の対応するテスト
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - 呼び出し元が渡したリスト・マップを後から変更しても record の中身が変わらないことを検証するテストがある
    - `DailyWish` の休みと時間帯が、それぞれ `Employee` の `off()`／`start()`／`end()`・`employmentType()` に正しく反映されることを検証するテストがある
    - `./mvnw test -Dtest=<追加したテストクラス>` が成功する

- [x] **T2. 日ごとの希望を優先順位どおりに決める（F-11、4.1 節）**
  - 依頼事項：`MonthlyShiftServiceImpl` の内部（または専用クラス `service/WishResolver`）に、ある営業日・ある従業員の希望を決める処理を作る。優先順位は「その日の個別変更 ＞ その曜日の基本シフト」（祝日・土日は営業日に含まれないので、ここでは扱わない）。個別変更は **従業員名と日付** で紐づける。従業員名が有効な従業員のどの氏名とも一致しない個別変更は **無視する（エラーにしない）**。同じ日付・同じ氏名の個別変更が複数あれば、リストで後ろのものを採用する。個別変更が基本シフトと同じ内容の場合は、結果として基本シフトと同じ希望になる
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/`（`WishResolver` を作る場合はそのファイル）、対応するテスト
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - 次を検証する `[F-11]` 付きのテストがある：個別変更がなければ曜日の基本シフト（例：火曜は火曜の設定）を使う／個別変更があれば基本シフトより優先する（休みへの変更、時間帯の変更の両方）／個別変更は日付が違う日には効かない／氏名が一致しない個別変更は無視される
    - `./mvnw test -Dtest=<追加したテストクラス>` が成功する

- [x] **T3. 営業日ごとに独立して割り当てを算出する（F-3、5 章）**
  - 依頼事項：`MonthlyShiftService` と `MonthlyShiftServiceImpl` を作る（`@Service`。コンストラクタで `HolidayService` と `ShiftAssignmentService` を受け取る）。`create` は、`HolidayService.businessDays(input.month())` の各営業日について、名前が空でない従業員（入力順のまま）の希望を T2 で決め、`Employee` のリストにして `ShiftAssignmentService.assign` を呼び、`DailyShiftResult` に詰めて営業日順の `MonthlyShiftResult` を返す。`availableCount` は、その日の有効な従業員のうち休みでない人の数。この段階では入力検証（V-2〜V-9）は行わなくてよい（T5〜T7 で追加する）。従業員名が空の行は除外する（V-1）
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/MonthlyShiftService.java`、`.../MonthlyShiftServiceImpl.java`、`src/test/java/com/example/shiftmatch/service/MonthlyShiftServiceImplTest.java`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - 次を検証する `[F-3]` 付きのテストがある：`businessDays` が返した日だけが結果に含まれ、順序も同じ／各営業日の結果が、その日の希望だけから算出した `assign` の結果と一致する（例：ある 1 日だけ全員の時間帯を変える個別変更を入れると、その日だけ結果が変わり、他の日は変わらない）／同点時は入力順（従業員リストの並び）で優先される（`assign` の既存規則が全営業日で共通に適用される）
    - 従業員名が空の行が結果（`assignment` の割り当て・未出勤者・`availableCount`）に含まれない `[V-1]` 付きのテストがある
    - `./mvnw test -Dtest=MonthlyShiftServiceImplTest` が成功する

- [ ] **T4. 勤務できる人が 8 名未満の日だけを不成立にする（V-4、F-5、6 章）**
  - 依頼事項：T3 の `create` で、ある営業日に勤務できる人（有効な従業員のうち休みでない人）が 8 名未満のとき、エラーにせず、その日の `DailyShiftResult` を `assignment` が空・`availableCount` に勤務できる人数を入れた状態で返す。ハード制約を満たす案がない日（8 名以上いても特定の枠に入れる人がいない日）も `assign` が空を返すので不成立になる。**他の営業日の算出は止めない**（例外を投げない）。`availableCount` は不成立でない日にも設定する
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/MonthlyShiftServiceImpl.java`、`src/test/java/com/example/shiftmatch/service/MonthlyShiftServiceImplTest.java`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - 次を検証する `[V-4]` `[F-5]` 付きのテストがある：ある 1 日だけ休みの人が増えて勤務できる人が 7 名になったとき、その日だけ `assignment` が空で `availableCount` が 7、他の日は成立している／ちょうど 8 名なら算出される（境界値）／8 名以上いるが特定の枠に誰も入れない日が不成立になり、他の日は成立する（`[6 章]`）
    - 例外が投げられないことを検証している
    - `./mvnw test -Dtest=MonthlyShiftServiceImplTest` が成功する

- [ ] **T5. 入力チェック V-1〜V-3 を実装する（V-1、V-2、V-3）**
  - 依頼事項：`MonthlyInputValidator` を作る（`@Component`）。`validate(MonthlyShiftInput)` は次を **この順** で検証し、`InputError` のリストを返す。エラーがなければ空リスト。`code` は仕様 ID（`"V-2"` など）、`message` は該当する従業員（行番号は 1 始まり）・曜日（月〜金）・日付を示す日本語の文言にする（既存の `templates/index.html` や `InvalidTimeRangeError` の文言・行番号の扱いを参考にする）
    - V-1：従業員名が空（null・blank）の行は、エラーにせず以降の検証・処理から除外する
    - V-2：有効な従業員の氏名の重複。該当行を示す。既存の `ShiftAssignmentService.findDuplicateNames` を再利用してもよい（`Employee.onLeave(name)` などに変換して渡す）
    - V-3：有効な従業員の基本シフト（月〜金の各曜日）と、有効な従業員に一致する個別変更のうち、「休み」でないもので、開始・終了が未選択（null）、7:30〜18:30 の 30 分単位以外の値、または開始が終了以上のもの。氏名が有効な従業員と一致しない個別変更は無視するため、V-3 の対象にしない。基本シフトの曜日が 5 つ揃っていない場合（`baseShifts` に月〜金の欠けがある）は、その曜日を未選択として V-3 のエラーにする
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/MonthlyInputValidator.java`、`src/test/java/com/example/shiftmatch/service/MonthlyInputValidatorTest.java`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `[V-1]` 空行が除外されエラーにならない、`[V-2]` 重複でエラーになり該当行が分かる、`[V-3]` 基本シフトの未選択・30 分単位でない値・開始 ≧ 終了、および個別変更の同様の不正がそれぞれエラーになる（エラーの `code` と、行または曜日・日付が `message` に含まれることを検証）テストがある
    - V-3 で「休み」の日は開始・終了が null でもエラーにならない、氏名不一致の個別変更が不正な値でもエラーにならないことを検証するテストがある
    - `./mvnw test -Dtest=MonthlyInputValidatorTest` が成功する

- [ ] **T6. 入力チェック V-5〜V-7 を実装する（V-5、V-6、V-7）**
  - 依頼事項：T5 の `MonthlyInputValidator.validate` に次を追加する（V-3 の後ろ、V-8 の前に評価される順序を保つ）
    - V-5：有効な従業員（休みを含む）が 13 名以上
    - V-6：従業員名が 255 文字を超える行（行番号を示す）
    - V-7：有効な従業員の雇用区分が「常勤・パート・管理職」以外（`employmentType` が null の場合など。`EmploymentType` の値と既存の `InvalidEmploymentTypeError` の扱いを参考にする）。行番号を示す
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/MonthlyInputValidator.java`、`src/test/java/com/example/shiftmatch/service/MonthlyInputValidatorTest.java`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `[V-5]` 12 名はエラーにならず 13 名でエラーになる（境界値）、`[V-6]` 255 文字はエラーにならず 256 文字でエラーになり行番号が分かる、`[V-7]` 区分が null の有効な従業員でエラーになり行番号が分かる、をそれぞれ検証するテストがある
    - `./mvnw test -Dtest=MonthlyInputValidatorTest` が成功する

- [ ] **T7. 入力チェック V-8・V-9 と検証順序、`create` への組み込み（V-8、V-9）**
  - 依頼事項：`MonthlyInputValidator`（コンストラクタで `HolidayService` を受け取る）に次を追加する。V-8：`HolidayService.isSupported(input.month())` が false の月はエラー。V-9：有効な従業員に一致する個別変更のうち、日付が対象月の営業日（`HolidayService.businessDays`）でないもの（土曜・日曜・祝日・対象月以外の日付）は、該当する日付を示すエラー。V-8 のエラーがあるときは `businessDays` を呼ばず（`HolidayDataUnavailableError` を避ける）、V-9 の検証はしない。氏名が有効な従業員と一致しない個別変更は V-9 の対象外（無視する）。次に `MonthlyShiftServiceImpl.create` の先頭で `MonthlyInputValidator.validate` を呼び、エラーがあれば `InvalidMonthlyInputException` を投げて **算出しない**（`ShiftAssignmentService.assign` を呼ばない）ようにする。V-4 はエラーにならない（T4 のとおり）ので、勤務できる人が 8 名未満でも例外にしない
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/MonthlyInputValidator.java`、`src/main/java/com/example/shiftmatch/service/MonthlyShiftServiceImpl.java`、対応するテスト
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `[V-8]` 判定できない月でエラーになり `businessDays` が呼ばれない、`[V-9]` 土曜日・祝日・対象月以外の日付の個別変更でエラーになり日付が `message` に含まれる、氏名不一致の個別変更は日付が営業日でなくてもエラーにならない、を検証するテストがある
    - 複数のエラーがあるとき、`errors()` が V-2 → V-3 → … → V-9 の順（`code` の番号順）に並ぶことを検証するテストがある
    - `create` が検証エラーで `InvalidMonthlyInputException` を投げ、`ShiftAssignmentService.assign` を一度も呼ばないことを検証する `[V-3]` `[V-9]` 付きのテストがある
    - `./mvnw test -Dtest=MonthlyInputValidatorTest+MonthlyShiftServiceImplTest` が成功する

- [ ] **T8. 営業日ごとの選定根拠ログに日付を添える（7.2 節）**
  - 依頼事項：`service/SelectionRationaleLogger`（`@Component`、SLF4J）を作る。`log(LocalDate date, DailyShiftResult result)` は、成立日には `ShiftController.logRationale`（`controller/ShiftController.java`）と同じ内容（各人の割当・希望・差・入れる枠、未出勤者の理由と入れる枠、「合計 = 90 + 30 + … = 240 分」）を **各行に日付（`yyyy-MM-dd`）を添えて** INFO で出力し、不成立日には日付と勤務できる人数を INFO で出力する。氏名の制御文字のエスケープ（`escapeControlCharacters`）も同じ規則で行う。**`ShiftController` は変更しない**（重複は #44 で画面を差し替える際に解消する）。`MonthlyShiftServiceImpl.create` は、算出した各営業日についてこのロガーを呼ぶ
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/SelectionRationaleLogger.java`、`MonthlyShiftServiceImpl.java`、`src/test/java/com/example/shiftmatch/service/SelectionRationaleLoggerTest.java`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - Logback の `ListAppender` でログを捕捉し、次を検証するテストがある：成立日の各ログ行に日付が含まれる／不成立日に日付と勤務できる人数が含まれる／氏名の改行が `\n` に変換され、偽のログ行が作られない／異なる 2 日のログが日付で区別できる
    - `MonthlyShiftServiceImpl.create` が営業日ごとにログを出すことを検証するテストがある
    - `./mvnw test -Dtest=SelectionRationaleLoggerTest+MonthlyShiftServiceImplTest` が成功する

- [ ] **T9. 全テストと静的解析を通す**
  - 依頼事項：`./mvnw spotless:apply` で整形し、`./mvnw test` を実行して、全テスト・Spotless・Checkstyle が成功することを確認する。失敗があれば原因を直す（仕様と異なる期待値へのテスト書き換え、`@Disabled` での回避はしない）。`pom.xml` に変更がないこと、`controller/`・`templates/`・`static/`・`persistence/` に変更がないことを `git diff main --stat` で確認する
  - 対象ファイル：本 Issue で変更した全ファイル
  - 完了条件：
    - `./mvnw test` で全テストが成功する（Spotless・Checkstyle の違反 0 件）
    - `git diff main --stat` に `pom.xml`・`controller/`・`templates/`・`static/`・`persistence/` が含まれない

## 実行ログ

<!-- implementer が試行結果（失敗理由・リトライ回数）を追記する欄。作成時は空のままにする -->
