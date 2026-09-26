# Todo: 対象月・従業員・基本シフト・個別変更の入力画面（月間 3/5）

- Issue: #44（親 Issue: #40）
- ブランチ: feature/44-monthly-input-screens
- 版: v3
- 対象仕様: F-1、F-2、F-6、F-8、F-9、F-11、8 章冒頭、8.1 節、8.2 節、8.5 節、V-1〜V-9（表示のみ）
- 作成日: 2026-09-26

## 前提

- 読むべきドキュメント・規約：`docs/specifications.md` の 3 章・4.1 節・4.2 節・8 章（8.1・8.2・8.5 節を特に丁寧に）、`.claude/rules/tdd.md`、`.agents/rules/` の `test.md`・`naming.md`・`javadoc.md`・`comment.md`・`exception.md`・`lambda.md`・`formatting.md`・`checkstyle.md`
- 本 Issue の範囲は **画面 1・2、ステップ操作、フォームバインディング、POST /shift の入り口**。次は対象外：
  - 結果の本実装（カレンダー・従業員別・日別詳細の 3 タブ）は #45。画面 3 は **暫定表示**（営業日ごとに「日付／成立 or 不成立／勤務できる人数」の簡易リスト）にとどめる
  - 保存・復元は #46。`persistence/`（`LatestShiftRepository` など）は変更しない。POST /shift は保存を呼ばない。GET / は保存済みの復元をしない（従業員 12 行の空行を用意し、対象月は今月）
  - `docs/`・`AGENTS.md` は編集できないので Todo に入れない（メインが更新する）
- **新しい依存ライブラリは追加しない**（`pom.xml` は変更しない）。メソッド参照（`Type::method`）は使わない（`lambda.md`）。フロントエンドフレームワークは使わない（素の JavaScript）
- **個別変更は、従業員の追加・削除・並べ替え・対象月の変更で破棄しない**（8.2 節。Issue #40 の「破棄」という記述は古い。仕様書が正）
- 既存コードとの関係
  - #43 で作った `service/MonthlyShiftService.create(MonthlyShiftInput)` が V-1〜V-9 の検証と算出を行う。検証エラーは `InvalidMonthlyInputException.errors()`（`InputError(code, message)` のリスト）で返る。この Issue でサーバー側検証を書き直さない。フォームの文字列は変換時に、解析できない値を `null` にして渡せば `MonthlyInputValidator` が V-3・V-7・V-8 として扱う（`null` の月・時刻・区分を受け付ける）
  - `domain/MonthlyShiftInput(YearMonth month, List<EmployeeProfile> employees, List<ShiftAdjustment> adjustments)`、`EmployeeProfile(String name, EmploymentType employmentType, Map<DayOfWeek, DailyWish> baseShifts)`、`ShiftAdjustment(LocalDate date, String employeeName, DailyWish wish)`、`DailyWish(boolean off, LocalTime start, LocalTime end)`。休みのとき `start`・`end` は `null`
  - `service/HolidayService`：`isSupported(YearMonth)`、`businessDays(YearMonth)`、`holidaysOf(YearMonth)`（日付→祝日名）。データがないとき `HolidayDataUnavailableError`
  - 時刻の選択肢（07:30〜18:30 の 30 分単位、`HH:mm`）は `controller/TimeOptions.VALUES`。`ShiftController` の `@ModelAttribute("timeOptions")`・`employmentTypes` は流用してよい
  - 現在の `controller/ShiftController`・`ShiftForm`・`EmployeeForm`・`ValidTimeRange`・`ValidTimeRangeValidator`・`templates/index.html`・`static/js/shift-form.js`・`static/css/shift-form.css` は 1 日分の画面用。**月間フォームに置き換える**（不要になったクラスと、そのテストは削除する。`ShiftControllerTest` は月間用に書き直す）。`ShiftAssignmentService`（1 日分の割り当て）は #43 の `MonthlyShiftServiceImpl` が使うので削除しない
  - `config/SameOriginInterceptor` は `POST /shift` の同一オリジン検証をするので、そのまま残す
- フォーム名は 8.5 節に従う：`targetMonth`（`YYYY-MM`）、`employees[i].name`、`employees[i].employmentType`（`FULL_TIME`／`PART_TIME`／`MANAGER`）、`employees[i].days[d].off`／`start`／`end`（d＝0 月〜4 金）、`adjustments[k].date`（`YYYY-MM-DD`）／`employeeName`／`off`／`start`／`end`。`start`・`end` は `HH:mm` の文字列。**インデックスに欠番を作らない**
- 画面の構成（`docs/specifications.md` 8 章・8.1・8.2 を正とする）：3 画面（画面 1 対象月と従業員／画面 2 日ごとの希望／画面 3 結果）を 1 つのフォームに入れ、JavaScript で表示を切り替える。切り替えだけではサーバーに送信しない。ページ全体は縦スクロールさせない（`html, body` は `height: 100%; overflow: hidden`。従業員一覧・カレンダー・入力パネル・結果だけが `overflow: auto`）
- JavaScript の検証：JS のテストフレームワークは導入しない。純粋なロジックは `static/js/` の各ファイルの先頭で関数として分け、構文は `node --check <ファイル>` で確認する（`node` は使える）。動作は MockMvc によるテンプレートの出力テスト（`name` 属性・`data-*` 属性・要素の有無）で担保する
- コミット前に `./mvnw spotless:apply`。コミットメッセージは Conventional Commits 形式の日本語（例：`feat: [F-9] 対象月の営業日数と祝日を返す JSON を追加する`）。コミットは Todo 1 件ごと。テストの `@DisplayName` の先頭に仕様 ID（例：`[F-9]`）を付け、Given-When-Then で書く

- **前回（v1）の失敗理由と今回の変更点**：v1 の実装は T1 の完了（コミット 255150a）で止まり、T2 は `CalendarController.java`・`CalendarResponse.java` が未コミットで残っている（テスト未完成）。T1 に時間を使いすぎたことが原因。今回は **T1 は完了済み**。T2 は、残っている未コミットのファイルを引き継いで `CalendarControllerTest` を先に整えて（RED を確認して）から完了させる。`ShiftControllerTest` は T1 の時点でコンパイルエラー部分がコメントアウトされ、`templates/index.html` は簡易版になっているが、それぞれ T3〜T5 で正しく書き直す（コメントアウトしたままにしない）。**各 Todo は最小限の実装で進め、1 件ずつ確実にコミットすること。時間が足りなくなったら、その時点までの完了・未完了を正確に報告する**

- **前回（v2）の失敗理由と今回の変更点**：v2 の実装は T4 の完了（コミット acc04bc）で止まった（実行時間・作業量の上限で中断したとみられる）。T1〜T4 は完了済み。今回は **T5〜T9 を残す**。v2 の時点で、`ShiftControllerTest` の一部が T1 でコメントアウトされたままか、`ShiftControllerMonthlyTest` として別ファイルに月間用テストが追加されている可能性がある。T5 で `ShiftControllerTest` に統合するか、二重になっていれば重複を整理し、コメントアウトしたテストを残さない。**T5〜T8 は 1 件ずつ最小限の実装で確実にコミットする。JS（T6〜T8）は 1 ファイルあたり 250 行程度を目安に簡潔に書く。途中で止まる場合でも、Todo ごとにコミットとチェックを済ませてから次へ進み、最後に完了・未完了を正確に報告すること**

## Todo

- [x] **T1. フォームの受け皿と、月間入力への変換を作る（8.5 節、V-1〜V-9 の入力側）**
  - 依頼事項：`controller/` に次を作る（すべてクラスに Javadoc。Lombok の `@Getter`／`@Setter` は既存の `ShiftForm`・`EmployeeForm` に倣う）。既存の `ShiftForm`・`EmployeeForm` は月間用に作り直す。
    - `ShiftForm`：`String targetMonth`、`List<EmployeeForm> employees`、`List<AdjustmentForm> adjustments`（既定は空の `ArrayList`）
    - `EmployeeForm`：`String name`、`String employmentType`、`List<DayForm> days`。**Bean Validation の注解（`@Size`・`@ValidTimeRange`）は付けない**（検証は `MonthlyInputValidator` に任せる）
    - `DayForm`：`boolean off`、`String start`、`String end`
    - `AdjustmentForm`：`String date`、`String employeeName`、`boolean off`、`String start`、`String end`
    - `MonthlyFormConverter`（`@Component`）：`MonthlyShiftInput toInput(ShiftForm form)`。`targetMonth` を `YearMonth.parse`（失敗・空・`null` は `null`）、`employmentType` は `EmploymentType.parse(...)`（未送信・3 択以外は `null`）、`days[d]` は `DayOfWeek` の月〜金（d＝0〜4）へ対応づけ、時刻は `HH:mm` として解析（空・不正・`off` が true のときは `null`。`off` が true なら `DailyWish(true, null, null)`）。`days` が 5 件に満たない曜日は `DailyWish(false, null, null)` として補う。`adjustments[k].date` は `LocalDate.parse`（失敗した個別変更は、日付が解析できないものとして扱えないため **除外しない**：`ShiftAdjustment` の `date` は `null` にせず、解析できない日付は `LocalDate.MIN` に置き換える。`V-9` で「営業日でない」エラーになる）。従業員名が空の行も除外せずそのまま渡す（V-1 は `MonthlyInputValidator`／`MonthlyShiftServiceImpl` が処理する）。個別変更の `employeeName` が `null` のときは空文字にする
    - 不要になる `controller/ValidTimeRange.java`・`ValidTimeRangeValidator.java` と、そのテスト `ValidTimeRangeValidatorTest` は削除する（この段階で `ShiftController` がコンパイルエラーになる場合は、`ShiftController` の該当部分をこの Todo の範囲で最小限だけ直して、テストが通る状態を保つ。`ShiftController` の本格的な書き直しは T4）
  - 対象ファイル：`src/main/java/com/example/shiftmatch/controller/ShiftForm.java`・`EmployeeForm.java`・`DayForm.java`・`AdjustmentForm.java`・`MonthlyFormConverter.java`、`src/test/java/com/example/shiftmatch/controller/MonthlyFormConverterTest.java`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `MonthlyFormConverterTest` に次を検証する `[F-1]`・`[F-11]`・`[V-3]`・`[V-7]`・`[V-8]` 付きのテストがある：正常な入力が `MonthlyShiftInput` に変換される（曜日の対応：`days[0]`＝月曜〜`days[4]`＝金曜、休みの曜日は `off=true` かつ `start`・`end` が `null`）／`targetMonth` が空・不正形式なら `month` が `null`／区分が 3 択以外なら `employmentType` が `null`／時刻が空・不正形式なら `null`／`days` が 5 件未満のとき不足する曜日が `DailyWish(false, null, null)`／個別変更が `ShiftAdjustment(date, employeeName, wish)` に変換され、日付が解析できないものは `LocalDate.MIN`／従業員名が空の行が除外されずに渡される
    - `./mvnw test -Dtest=MonthlyFormConverterTest` が成功し、`./mvnw test-compile` が成功する

- [x] **T2. 対象月の営業日数と祝日を返す JSON を作る（F-9）**
  - 依頼事項：画面 1 の月の切り替えで、フォームを送信せずに営業日数・祝日を得るための `GET /calendar?month=YYYY-MM` を `controller/CalendarController`（`@RestController`）に作る。`HolidayService` を使う。応答（JSON、Spring Boot 標準の Jackson で `record` を返す。新しい依存は不要）：`{"month":"2026-10","businessDayCount":22,"holidayCount":1,"businessDays":["2026-10-01",...],"holidays":[{"date":"2026-10-12","name":"スポーツの日"}]}`。`holidayCount` は、祝日のうち **月〜金にあたるものの数** ではなく、`holidaysOf` が返した件数そのまま（土日の祝日も含む）。`month` が `YYYY-MM` として解析できない、または `HolidayService.isSupported` が false のときは HTTP 400 と `{"message":"対象月を判定できません。祝日データにない月です。"}` を返す（V-8 と同じ趣旨）。応答用の `record`（例：`CalendarResponse`、`HolidayEntry`）は `controller/` に置く
  - 対象ファイル：`src/main/java/com/example/shiftmatch/controller/CalendarController.java`（と応答 `record`）、`src/test/java/com/example/shiftmatch/controller/CalendarControllerTest.java`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `@WebMvcTest(CalendarController.class)` と `@MockitoBean HolidayService` で、次を検証する `[F-9]` 付きのテストがある：営業日の一覧・営業日数・祝日（日付と名前）・祝日数が JSON で返る／`month` が不正形式のとき 400／`isSupported` が false のとき 400 でメッセージが返る
    - `./mvnw test -Dtest=CalendarControllerTest` が成功する

- [x] **T3. GET / を月間フォームの初期表示にする（F-1、F-2、8.1 節）**
  - 依頼事項：`ShiftController` を月間用に書き直す（この Todo では `GET /` のみ。`POST /shift` は T4）。コンストラクタは `MonthlyShiftService` と `MonthlyFormConverter` を受け取る（`ShiftAssignmentService`・`LatestShiftRepository` への依存は外す。`ShiftControllerTest` の `@MockitoBean` もそれに合わせて直す）。`GET /` は `ShiftForm` を作り、`targetMonth` を今月（`YearMonth.now()` を `YYYY-MM` で）、`employees` を **12 行**（名前は空、区分は `FULL_TIME`、`days` は月〜金 5 件で休みなし・`07:30`〜`18:30`）、`adjustments` は空にしてモデル `shiftForm` に入れ、`index` を返す。`@ModelAttribute("timeOptions")`・`employmentTypes` は残す。旧 `ShiftControllerTest` の 1 日分向けのテストは、この Todo で月間用に書き直すか削除する（仕様と異なる期待値へ書き換えない。1 日分の振る舞いを検証していたものは削除する）
  - 対象ファイル：`src/main/java/com/example/shiftmatch/controller/ShiftController.java`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `@WebMvcTest(ShiftController.class)` のテストに、次を検証する `[F-1]`・`[F-2]`・`[F-9]` 付きのものがある：`GET /` が 200 で `index` を返す／モデル `shiftForm` の `employees` が 12 行で、各行の区分が `FULL_TIME`、`days` が 5 件で `07:30`〜`18:30`・休みなし／`targetMonth` が `YYYY-MM` 形式で今月／`timeOptions` が 07:30〜18:30 の 30 分刻み 23 件
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する（この時点でテンプレートは旧版のままで、`th:field` の参照先がなくなり描画に失敗する場合は、`ShiftControllerTest` の `GET /` のテストはモデルの検証だけにとどめ、`@WebMvcTest` のビュー描画が原因で失敗しないようにする。テンプレートは T5 で置き換える）

- [x] **T4. POST /shift で月間の入力を受け取り、エラーを画面 1 の上部にまとめる（F-3 の入り口、V-1〜V-9 の表示）**
  - 依頼事項：`ShiftController` に `@PostMapping("/shift")` を実装する。`ShiftForm` を `@ModelAttribute("shiftForm")` で受け取り、`MonthlyFormConverter.toInput` → `MonthlyShiftService.create` を呼ぶ。`employees` が空で送られたときは 1 行の空行（区分は `FULL_TIME`、`days` 5 件）を補う。成功時：モデルに `monthlyResult`（`MonthlyShiftResult`）と `initialStep=3` を入れて `index` を返す。`InvalidMonthlyInputException` のとき：モデルに `inputErrors`（`List<InputError>`。順序は例外の順のまま）と `initialStep=1` を入れて `index` を返し、`monthlyResult` は入れない。`GET /` では `initialStep=1`。`shiftForm` は常にモデルに戻す（入力を残すため）。保存（`LatestShiftRepository`）は呼ばない
  - 対象ファイル：`src/main/java/com/example/shiftmatch/controller/ShiftController.java`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `MonthlyShiftService` を `@MockitoBean` にした MockMvc テストに、次を検証する `[F-3]`・`[V-2]`・`[V-3]`・`[V-9]` 付きのものがある：正常な送信で `create` が呼ばれ、渡された `MonthlyShiftInput` の `month`・従業員（名前・区分・曜日ごとの基本シフト）・個別変更がフォームの内容どおりである（`ArgumentCaptor`）／成功時にモデルへ `monthlyResult` と `initialStep=3`／`InvalidMonthlyInputException`（`V-2`・`V-3`・`V-9` を含む 3 件）を投げさせると、モデルの `inputErrors` にその 3 件が同じ順で入り `initialStep=1` になり `monthlyResult` がない／エラー時も `shiftForm` の入力値が保持される／`employees` を 1 件も送らないと空行が 1 行補われる
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する

- [x] **T5. テンプレート（3 画面の骨格・入力行・エラー表示）を作る（8 章、8.1 節、8.5 節）**
  - 依頼事項：`templates/index.html` を月間用に書き直す（`th:object="${shiftForm}"`、`th:field` を使う。`static/css/shift-form.css`・`static/js/` から読み込む）。構成は次のとおり。
    - 上部：ステップ表示（「1 対象月と従業員」「2 日ごとの希望」「3 結果」。ボタンで、`data-step="1"〜"3"`）。`<body>` または `.app` に `data-initial-step="${initialStep}"`（未設定は 1）
    - 画面 1 の上部に、`inputErrors` を 1 つの `<section class="alert" role="alert">` に集約して表示する（`<li>` に `error.code()` と `error.message()`）。エラーがなければ出さない
    - 画面 1（`<section id="screen-1" data-screen="1">`）：対象月の切り替え（`◀` `▶` ボタンと、`name="targetMonth"` の `<input type="hidden">`、表示ラベル、営業日数・祝日数を出す要素 `id="month-summary"`）。左に従業員一覧（列：▲▼／氏名／区分／基本シフト要約／削除）、一覧の下に「行を追加」ボタン。右に基本シフトパネル（選んだ従業員の曜日 5 行：曜日名・休み・開始・終了・「全曜日へ」ボタン）
    - 従業員一覧の各行 `<tr class="employee-row" data-row-id="行の識別子">`：▲▼ボタン（`.move-up-btn`・`.move-down-btn`）、氏名（`employees[i].name`）、区分（`employees[i].employmentType`、選択肢は `employmentTypes`）、基本シフト要約（`.summary`。初期表示は `07:30〜18:30` など）、削除ボタン（`.delete-btn`）。基本シフトの入力（`employees[i].days[d].off`／`start`／`end`、d＝0〜4）は従業員 1 人ごとに `<div class="base-panel" data-row-id="同じ識別子" hidden>` を `#base-panels` に並べる（右パネルは、選んだ行の `.base-panel` だけを表示する）。`th:each` で `shiftForm.employees` の件数分を出す
    - 画面 2（`<section id="screen-2" data-screen="2" hidden>`）：左に `#calendar`（JS が描画）、右に `#day-panel`（JS が描画）、`#adjustment-inputs`（個別変更の hidden 入力の置き場。`shiftForm.adjustments` を `th:each` で `adjustments[k].date`／`employeeName`／`off`／`start`／`end` の hidden にして最初から入れておく）
    - 画面 3（`<section id="screen-3" data-screen="3" hidden>`）：`monthlyResult` があるときだけ、暫定の簡易リスト（営業日ごとに日付、成立／不成立、勤務できる人数 `availableCount`）を出す。ないときは「結果はまだありません」
    - 下部：「戻る」「次へ」ボタン（`id="prev-btn"`・`id="next-btn"`。画面 2 の「次へ」は JS が「1 か月分のシフトを作成」に変える `type="submit"` 相当）
    - `data-time-options` は `#strings.listJoin(timeOptions, '|')` として `<form>` に持たせる
    - `<form method="post" th:action="@{/shift}">` は 1 つ。JS は `static/js/` の複数ファイルに分ける（T6〜T8 で作る）：`step-nav.js`、`employee-list.js`、`day-adjustments.js`。テンプレートはこの 3 つを `<script defer>` で読み込む
  - 対象ファイル：`src/main/resources/templates/index.html`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`（描画の検証を追加）
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - MockMvc で `GET /` の HTML 本文（`content().string(...)`）を検証する `[F-1]`・`[F-2]`・`[F-6]`・`[F-8]`・`[F-9]` 付きのテストがある：`name="targetMonth"`、`name="employees[0].name"`、`name="employees[11].name"`、`name="employees[11].employmentType"`、`name="employees[0].days[0].off"`、`name="employees[11].days[4].end"` がある／`name="employees[12].name"` は無い／各従業員行に `.move-up-btn`・`.move-down-btn`・`.delete-btn` がある／`id="screen-1"`・`id="screen-2"`・`id="screen-3"`・`id="prev-btn"`・`id="next-btn"` がある
    - 入力エラーの表示を検証する `[V-3]` 付きのテストがある：`MonthlyShiftService` に `InvalidMonthlyInputException`（`V-3` と `V-2` のエラー）を投げさせて `POST /shift` すると、HTML の画面 1（`id="screen-1"` より前、または画面 1 の内側の先頭）に `role="alert"` の要素があり、そこに各エラーの `code` とメッセージが含まれる。エラーがないときは `role="alert"` が無い
    - 個別変更の hidden 入力の復元を検証するテストがある：`adjustments[0].date` などを含めて `POST /shift`（`MonthlyShiftService` は例外を投げさせる）すると、HTML に `name="adjustments[0].date"` の値が入っている
    - 画面 3 の暫定表示を検証するテストがある：`MonthlyShiftService` に営業日 2 日分（成立 1、不成立 1）の `MonthlyShiftResult` を返させて `POST /shift` すると、日付と「不成立」と勤務できる人数が HTML に含まれる
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する

- [x] **T6. ステップ操作とページ全体の非スクロール（F-9、8 章冒頭・8.1 節の対象月切り替え）**
  - 依頼事項：`static/js/step-nav.js` を作る。①ステップ表示・「戻る」「次へ」で `[data-screen]` を切り替える（切り替えでは送信しない。`data-initial-step` の画面から始める）。画面 1 では「戻る」を無効、画面 2 の「次へ」は「1 か月分のシフトを作成」に文言を変えてフォームを送信する（画面 3 は「戻る」のみで、「次へ」は非表示）。ステップ表示を押すと、その画面へ移動できる。②対象月の切り替え：`◀`／`▶` で `targetMonth`（hidden、`YYYY-MM`）を前後の月にして、表示ラベル（`2026 年 10 月`）を更新し、`fetch("/calendar?month=YYYY-MM")` の応答（T2）で `#month-summary` に「営業日 22 日・祝日 1 日」を表示する。取得に失敗（400 など）したら、メッセージを `#month-summary` に表示する。取得した営業日・祝日は `document` に `CustomEvent("calendar-loaded", {detail: 応答})` で通知する（画面 2 が使う）。ページ読み込み時にも 1 回取得する。③`static/css/shift-form.css` を書き直し、`html, body { height: 100%; overflow: hidden; }`、`.app` を縦並び（上部ステップ表示・中央の画面領域・下部ボタン）にして、中央は残りの高さを使い、従業員一覧・カレンダー・入力パネル・結果（`.scroll`）だけを `overflow: auto` にする。既存の見た目（色・カード・ボタン）の CSS は流用してよい
  - 対象ファイル：`src/main/resources/static/js/step-nav.js`、`src/main/resources/static/css/shift-form.css`
  - 完了条件：
    - `node --check src/main/resources/static/js/step-nav.js` が成功する
    - ファイルに、`data-initial-step`・`fetch(` と `/calendar?month=`・`calendar-loaded`・`prev-btn`・`next-btn` の記述がある（`grep` で確認）
    - `shift-form.css` に `overflow: hidden`（`html, body`）と、`.scroll { ... overflow: auto }` に相当する記述がある
    - `./mvnw test` が成功する（Todo T1〜T5 のテストが壊れていない）

- [x] **T7. 従業員一覧と基本シフトパネル（F-2、F-6、F-8、8.1 節、8.5 節）**
  - 依頼事項：`static/js/employee-list.js` を作る。純粋なロジックは先頭の関数に分ける（`renumber`・`summarize` など。`node` で単体確認できる形）。
    - **行の追加**：`#employee-rows` の末尾に空行を追加する（区分は常勤、基本シフトは休みなし・07:30〜18:30）。行と対の `.base-panel`（`#base-panels`）も同時に作り、`data-row-id` を新しい識別子（連番のカウンターで一意にする）にする。時刻の選択肢は `<form>` の `data-time-options`（`|` 区切り）から作る。12 行のときは「行を追加」ボタンを無効にする
    - **行の削除**：行と、対の `.base-panel` を消す。1 行だけのときは削除ボタンを無効にする（最低 1 行を残す）
    - **並べ替え**：▲▼で行を入れ替える（`.base-panel` は `data-row-id` で対応しているので、DOM の順序が変わらなくてもよいが、`name` の番号は行の並びに合わせる）。先頭行の▲・末尾行の▼は無効にする。氏名・区分・基本シフトの入力値は行と一緒に移動する（行ごと動かすので値は保たれる）
    - **インデックスの振り直し**：追加・削除・並べ替えのたびに、各行 `i`（0 から連番）について、行の入力（`employees[i].name`／`employmentType`）と、対の `.base-panel` の入力（`employees[i].days[d].off`／`start`／`end`）の `name` を `i` に振り直す。欠番を作らない
    - **基本シフトパネル**：行をクリック（または行内の入力にフォーカス）すると、その行を選択状態にし、右の `.base-panel` だけを表示する（他は `hidden`）。初期は先頭行が選択される。パネルの曜日行では、「休み」にチェックした曜日の開始・終了を無効にする（送信されなくなるので、サーバーは休みの曜日の時刻を見ない。仕様上問題ない）。「全曜日へ」ボタンは、その曜日の休み・開始・終了を、全曜日へコピーする
    - **要約**：行の `.summary` を、`07:30〜18:30／休：水` の形式で更新する。休みでない曜日の開始・終了がすべて同じならその 1 つを、異なるなら、最も早い開始〜最も遅い終了を表示し、休みの曜日があれば `／休：水` のように曜日名を並べる（曜日がすべて休みなら `休：月火水木金`）。基本シフトの入力・氏名の入力が変わったら更新する
    - 従業員の追加・削除・並べ替え・氏名の変更を、`document` に `CustomEvent("employees-changed")` で通知する（画面 2 が使う）
  - 対象ファイル：`src/main/resources/static/js/employee-list.js`
  - 完了条件：
    - `node --check src/main/resources/static/js/employee-list.js` が成功する
    - `renumber` と `summarize` に相当する関数を、DOM に依存しない形で書き、`node -e` で次を確認した：3 行の `name` がずれなく 0,1,2 に振り直される／`summarize` が `07:30〜18:30／休：水` の形式を返す（実行した確認内容を「実行ログ」に書く）
    - ファイルに、`employees-changed`・`.move-up-btn`・`.move-down-btn`・`.delete-btn`・`base-panel`・`data-row-id` の記述がある（`grep` で確認）
    - `./mvnw test` が成功する

- [ ] **T8. 日ごとの希望（営業日カレンダー・個別変更）（F-11、8.2 節）**
  - 依頼事項：`static/js/day-adjustments.js` を作る。純粋なロジックは先頭の関数に分ける。
    - **カレンダー**：`calendar-loaded`（T6）の応答（`businessDays`・`holidays`）を使い、`#calendar` に、対象月の営業日を月〜金の週ごとに並べて描画する（週の最初の行は、月曜より前の日を空欄にする）。祝日は祝日名を表示して選択不可（`disabled`／クリック無効）。土日は表示しない。個別変更がある日は色を変え（CSS クラス `has-change`）、「変更 n 名」と表示する。日付をクリックすると選択され、`#day-panel` にその日の入力が出る（初期は最初の営業日）
    - **選択日の入力（`#day-panel`）**：有効な従業員（氏名が空でない行）ごとに、氏名・休み・開始・終了を出す（この入力には `name` 属性を付けない。送信は `#adjustment-inputs` の hidden だけで行う）。初期値はその日の曜日の基本シフト（`employees[i].days[d]` を DOM から読む）か、その従業員のその日の個別変更があればその内容。**基本シフトと同じ内容（休み／開始・終了）にすると、その従業員の個別変更を削除する**。同じでなければ設定する。「この日を基本に戻す」ボタンで、その日の個別変更をすべて消す
    - **個別変更の保持**：個別変更は、内部の `Map`（キーは `日付|従業員名`、値は `{off, start, end}`）で持つ。**従業員の追加・削除・並べ替え・氏名の変更、対象月の変更で `Map` を消さない**（`employees-changed` では `#day-panel` を再描画するだけ）。ページ読み込み時に、`#adjustment-inputs` の hidden 入力（サーバーからの復元）を `Map` に取り込む
    - **送信**：`Map` が変わるたび、そして送信の直前に、`#adjustment-inputs` の中身を作り直す。対象月（`targetMonth`）の日付の個別変更だけを、`adjustments[k].date`／`employeeName`／`off`／`start`／`end` として、k を 0 から連番で hidden 入力にする（他の月の個別変更は `Map` に残すが送らない。V-9 で誤ってエラーにならないため）。`off` は `true` のときだけ送る
  - 対象ファイル：`src/main/resources/static/js/day-adjustments.js`、`src/main/resources/static/css/shift-form.css`（`has-change`・選択日・祝日のスタイル）
  - 完了条件：
    - `node --check src/main/resources/static/js/day-adjustments.js` が成功する
    - 個別変更の判定・組み立てを DOM に依存しない関数で書き、`node -e` で次を確認した：基本シフトと同じ内容にすると個別変更が消える／違う内容なら残る／「基本に戻す」でその日の個別変更が全部消える／対象月以外の日付の個別変更が送信用の一覧に含まれず、`Map` には残る／送信用の一覧のインデックスが 0 から連番（実行した確認内容を「実行ログ」に書く）
    - ファイルに、`calendar-loaded`・`employees-changed`・`has-change`・`変更`・`この日を基本に戻す`・`adjustments[` の記述がある（`grep` で確認）
    - `./mvnw test` が成功する

- [ ] **T9. 全テストと静的解析を通し、画面を実際に動かして確認する**
  - 依頼事項：`./mvnw spotless:apply` で整形し、`./mvnw test` を実行して、全テスト・Spotless・Checkstyle が成功することを確認する。失敗があれば原因を直す（仕様と異なる期待値へのテスト書き換え、`@Disabled` での回避はしない）。使われなくなったクラス・テスト（1 日分の画面専用のもの）が残っていないことを確認する（`ShiftController` が `ShiftAssignmentService`・`LatestShiftRepository` を使っていないこと）。`pom.xml`・`persistence/` に変更がないこと、`node --check src/main/resources/static/js/*.js` がすべて成功することを確認する
  - 対象ファイル：本 Issue で変更した全ファイル
  - 完了条件：
    - `./mvnw test` で全テストが成功する（Spotless・Checkstyle の違反 0 件）
    - `git diff main --stat` に `pom.xml`・`persistence/`・`docs/`・`AGENTS.md` が含まれない
    - `node --check` が `static/js/` の全ファイルで成功する

## 実行ログ

- T5 試行 1/1：成功 — テンプレート構造（3画面・error alert・adjustment inputs）完成、T5テスト 7/7 合格
- T6 試行 1/1：成功 — step-nav.js と CSS 更新完成、月切り替え機能と calendar-loaded イベント実装
- T7 試行 1/1：成功 — employee-list.js 完成、renumber と summarize 関数動作確認、employees-changed イベント実装
