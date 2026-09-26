# Todo: 従業員の雇用区分（常勤／パート／管理職）の入力・保存・表示

- Issue: #38
- ブランチ: feature/38-employment-type
- 版: v2
- 対象仕様: F-1, F-4, F-7, V-7（新設）、H-1〜H-3・5.2・5.3（不変の確認）、T-5
- 作成日: 2026-09-26

## 前提

- 読むべきドキュメント・規約：`AGENTS.md`、`docs/specifications.md`（2 章末尾・4 章・8 章）、`.claude/rules/tdd.md`、`.agents/rules/` の全ファイル（特に `test.md`・`naming.md`・`javadoc.md`）
- 決定事項（ユーザー承認済み）
  - 区分は 3 択：常勤（`FULL_TIME`）・パート（`PART_TIME`）・管理職（`MANAGER`）。「役員」は「管理職（社長・部長など）」に改称する
  - 区分は必須。初期値は常勤（空行・追加行も常勤）。未選択の選択肢は設けない
  - **区分はシフト割り当てロジックに一切影響させない**（H-1〜H-3、スコア、同点時の規則は変更しない。区分ごとの扱いは別の仕様変更）
  - V-7：従業員名が入力された行（有効な従業員）の区分が 3 択以外の値ならエラー。該当行を示して表示し、算出も保存もしない。検証順は V-1 → V-7。従業員名が空の行は V-1 により除外するので区分を検査しない
- 実装上の注意
  - フォームの値と DB の値には列挙型の名前（`FULL_TIME` など）を使い、画面の表示にはラベル（常勤・パート・管理職）を使う
  - `Employee` は `record Employee(String name, EmploymentType employmentType, boolean off, LocalTime start, LocalTime end)` とする。既存の `Employee.working(name, start, end)` と `Employee.onLeave(name)` は区分を常勤として作る（シグネチャ不変）。区分を指定できるオーバーロードも足す。既存テストを壊さないため、旧 4 引数コンストラクタ `Employee(name, off, start, end)`（常勤を補う）も残してよい
  - **H2 は `./data/shift-match` のファイルモードで永続化されており、`CREATE TABLE IF NOT EXISTS` は既存テーブルに列を足さない**。`schema.sql` に `ALTER TABLE saved_employee ADD COLUMN IF NOT EXISTS employment_type VARCHAR(16) DEFAULT 'FULL_TIME' NOT NULL;` を `CREATE TABLE` の後に追加し、既存 DB でも起動できるようにする（新しい依存ライブラリは追加しない）
  - `seed.sql` の `INSERT` にも `employment_type` を加える（12 名に常勤・パート・管理職を混ぜる）。`SeedDataTest` の期待値も更新する
  - 行の並べ替え・削除・追加で `name` のインデックスを振り直す JS（`src/main/resources/static/js/shift-form.js`）が、新しい `employmentType` の `select` も追従するようにする（`employees[N].employmentType`）
  - ログ出力（`logRationale`）は変更しない

- 前回（v1）の失敗理由：`implementer` が T4 まで完了した時点で作業を止めて【未完了】を報告した。その際、Todo ファイルの構造が壊れた（T1 の依頼事項が T4 の下に移り、T2〜T4 が未完了のまま重複）。なお報告にあった「`Employee` の旧 4 引数コンストラクタが無く `ShiftControllerTest` で約 66 件のコンパイルエラー」は誤りで、`./mvnw test-compile` は成功する（旧 4 引数コンストラクタは残っている）
- 今回の変更点：T1〜T4 は完了済み（コミット 96160dd・f81083f・ecebca2・6c3ce0b）として `[x]` で引き継ぎ、依頼事項は省略。T5〜T10 は v1 と同じ
- 進め方の注意：Todo ファイルは **`- [ ]` を `- [x]` に変える以外の編集をしない**（行の移動・複製・削除をしない）。T5〜T10 を止まらずに最後まで実施し、途中で報告しない。実行ログには失敗と試行回数だけを追記する

## Todo

- [x] **T1. 雇用区分の列挙型 `EmploymentType` を追加する**（完了・96160dd）
- [x] **T2. `Employee` に区分を持たせる**（完了・f81083f）
- [x] **T3. 区分によって割り当て結果が変わらないことをサービスのテストで固定する**（完了・ecebca2）
- [x] **T4. スキーマとリポジトリで区分を保存・復元する（F-7）**（完了・6c3ce0b）
- [x] **T5. フォームで区分を受け取り、`Employee` に変換する（初期値は常勤）**
  - 依頼事項：`EmployeeForm` に `String employmentType` を追加し、初期値を `"FULL_TIME"` にする（空行・追加行が常勤になる）。`ShiftController#convertToEmployees` で `EmploymentType.parse` を使って `Employee` を作る（3 択以外の行は、T6 で検出できるよう、区分が不正であることを保持しておく。方法は既存の設計に合わせて選んでよい）。`index` の復元処理で、保存済みの区分をフォームに詰める
  - 対象ファイル：`src/main/java/com/example/shiftmatch/controller/EmployeeForm.java`、`ShiftController.java`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - テストを先に書き、RED を確認した
    - `GET /` で、保存がないとき 12 行すべての区分が `FULL_TIME` になるテストと、保存済みの区分（管理職・パート）が復元されるテストがある（`[F-7]`）
    - `POST /shift` に有効な入力と区分 `MANAGER` を送ると、`LatestShiftRepository#save` に渡る `Employee` の区分が `MANAGER` になるテストがある
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
- [ ] **T6. 入力チェック V-7（区分が 3 択以外ならエラー）を追加する**
  - 依頼事項：従業員名が入力された行で区分が 3 択以外（未指定・空・`EXECUTIVE` など）のとき、`POST /shift` は算出も保存もせず、`index` を返して該当行番号つきのエラーを表示する。エラー表示は既存の `nameErrors`・`timeRangeErrors` と同じ方式（新しいレコードとモデル属性を追加し、`index.html` にセクションを追加）とする。従業員名が空の行は区分を検査しない。エラーメッセージ例：「N 行目 雇用区分は「常勤」「パート」「管理職」から選択してください。」
  - 対象ファイル：`src/main/java/com/example/shiftmatch/controller/ShiftController.java`、`src/main/java/com/example/shiftmatch/domain/InvalidEmploymentTypeError.java`（新規）、`src/main/resources/templates/index.html`、`ShiftControllerTest.java`
  - 完了条件：
    - テストを先に書き、RED を確認した
    - `@DisplayName` の先頭に `[V-7]` を付けた次のテストがある：不正な区分の行でエラーメッセージ（行番号つき）が表示される／そのとき `LatestShiftRepository#save` も `ShiftAssignmentService#assign` も呼ばれない／氏名が空の行の不正な区分はエラーにならない／ほかの検査（V-2〜V-6）のエラーと同時に発生しても各エラーが表示される
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
- [ ] **T7. 入力画面に「区分」列を追加する（初期値は常勤・行の追加／並べ替え／削除に追従）**
  - 依頼事項：`index.html` の入力表に、氏名の右へ「区分」列（`th:field="*{employees[__${stat.index}__].employmentType}"` の `select`。選択肢は `EmploymentType` の 3 値で、表示はラベル、値は列挙名）を追加する。選択肢はコントローラーの `@ModelAttribute("employmentTypes")` で渡す。`shift-form.js` の行追加（`addRow`）にも同じ `select`（初期値 常勤）を加え、インデックス振り直し処理が `employmentType` も対象にするよう確認・修正する。並べ替え（F-8）では区分の値も行と一緒に移動すること。CSS（`shift-form.css`）は入力表の列幅が崩れないよう最小限だけ調整する
  - 対象ファイル：`src/main/resources/templates/index.html`、`src/main/resources/static/js/shift-form.js`、`src/main/resources/static/css/shift-form.css`、`ShiftController.java`、`ShiftControllerTest.java`
  - 完了条件：
    - テストを先に書き、RED を確認した（画面の HTML を検証する `ShiftControllerTest` の MockMvc テストで、`select` の `name="employees[0].employmentType"`、3 つの `option`、初期の選択が常勤であることを確認）
    - エラー再表示時（`POST /shift` が入力エラーで `index` を返したとき）も、送信した区分が選択されたまま表示されるテストがある
    - `shift-form.js` の行追加・振り直し・並べ替えが区分の `select` を扱うコードになっている（JS に自動テストがないため、変更箇所を実行ログに記す。可能ならアプリを起動して確認する）
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
- [ ] **T8. 割当結果画面に区分を表示する（F-4）**
  - 依頼事項：割当結果の表の「氏名」の右に「区分」列を追加して区分のラベルを表示する。未出勤者のチップにも区分のラベルを併記する（例：`佐藤 太郎（パート）`）。時間軸バーは変更しない。判定ロジックは変えない
  - 対象ファイル：`src/main/resources/templates/index.html`、`ShiftControllerTest.java`
  - 完了条件：
    - テストを先に書き、RED を確認した
    - 成立時の HTML に、割り当てられた各人の区分ラベル（常勤・パート・管理職）が結果表に含まれるテストがある（`[F-4]`）
    - 未出勤者の一覧にも区分ラベルが含まれるテストがある
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
- [ ] **T9. 仕様書・README・AGENTS.md を更新する**
  - 依頼事項：次を実装内容に合わせて更新する。仕様の意味を変えない範囲の表記統一は行ってよい。それ以外の仕様の変更は行わない
    - `docs/specifications.md`：2 章末尾の「従業員の区分（正社員・役員・パート）は本書の対象外…」を、区分（常勤・パート・管理職。管理職は社長・部長など）を入力・保存・表示するが割り当てには使わない旨（T-5 の決定内容）に書き換える。4.1 の入力項目に「雇用区分」（必須・既定は常勤）を追加。4.2 に V-7 を追加し、検証順を V-1 → V-7 にする（本文・8 章の保存の記述の「V-1〜V-6」も V-1〜V-7 に）。3 章の F-1・F-4・F-7 の概要に区分を加える。7 章の出力仕様の割り当て表に区分を加える。8 章の入力表の列（並べ替え・従業員名・区分・休み・開始・終了・削除）と `name` 属性の例に `employees[0].employmentType` を追加する。10 章末尾の T-5 の説明を確認する
    - `docs/requirements.md`：53 行付近の「正社員 5・役員 2・パート 5」を「常勤・パート・管理職」の表記へ揃え、区分は入力・保存・表示のみで割り当てには使わない旨にする。用語（8 章）に「雇用区分」を追加する
    - `AGENTS.md`：ドメインの要点にある区分の記述、入力チェックに V-7、フォームバインディングの例に `employmentType` を追記する
    - `README.md`：区分に関する記述があれば更新し、なければ機能の説明に区分の入力を加える
  - 対象ファイル：`docs/specifications.md`、`docs/requirements.md`、`AGENTS.md`、`README.md`
  - 完了条件：
    - `grep -rn "役員\|正社員" docs README.md AGENTS.md src` の結果が 0 件、または残る箇所が意図した記述（管理職の説明として「役員」を含める場合など）で、実行ログに理由がある
    - `grep -rn "V-1〜V-6\|V-1 → V-6" docs README.md AGENTS.md src` の結果が 0 件である
    - `docs/specifications.md` に V-7 の行と、区分は割り当てに影響しない旨の記述がある
- [ ] **T10. 全テストと静的解析を通す**
  - 依頼事項：`./mvnw spotless:apply` で整形してから `./mvnw test` を実行し、失敗・違反があれば直す（振る舞いの変更を伴う場合は該当 Todo の TDD からやり直す）。区分が割り当てに影響していないことを、差分（`git diff main -- src/main/java/com/example/shiftmatch/service src/main/java/com/example/shiftmatch/domain/BreakScheduler.java`）で確認する
  - 対象ファイル：変更した全ファイル
  - 完了条件：
    - `./mvnw test` が成功し、Spotless・Checkstyle の違反が 0 件である
    - `git diff main -- src/main/java/com/example/shiftmatch/service src/main/java/com/example/shiftmatch/domain/BreakScheduler.java` に、割り当て・スコア・同点の判定ロジックの変更がない（`Employee` の生成箇所の修正を除く）

## 実行ログ

<!-- implementer が試行結果（失敗理由・リトライ回数）を追記する欄。作成時は空のままにする -->
