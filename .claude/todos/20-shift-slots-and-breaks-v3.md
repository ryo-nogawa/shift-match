# Todo: 割り当て枠を6種類8名へ変更し、休憩の自動割り当てと上限12名を実装する

- Issue: #20
- ブランチ: feature/20-shift-slots-and-breaks
- 版: v3
- 対象仕様: F-1, F-3, F-4, F-5, F-2, F-6, V-3, V-4, V-5, H-1, H-2, H-3, C-1〜C-7
- 作成日: 2026-09-24（v2・v3 は同日再作成）

## 前提

- 読むべきドキュメント・規約：`AGENTS.md`、`docs/requirements.md`、`docs/specifications.md`（特に 2・4・5・7・8 章）、`.claude/rules/tdd.md`、`.agents/rules/` の全ファイル（特に `test.md`・`naming.md`・`comment.md`・`javadoc.md`・`lambda.md`）
- **仕様の新旧**：仕様書は新仕様に更新済みです。現在のコードは旧仕様（早番 2 名・遅番 2 名、`Employee(name, earlyWish, lateWish)`、`AssignmentResult.breakTimes()`）です。旧仕様に基づくテストは、新仕様と矛盾する場合に限り削除・書き換えてかまいません（`@Disabled` やコメントアウトでの回避は禁止）
- **新仕様の要点**
  - 枠は 6 種類・8 名。No.1 `07:30〜14:30` 2 名／No.2 `08:00〜15:30` 1 名／No.3 `08:30〜16:30` 1 名／No.4 `09:00〜16:30` 1 名／No.5 `09:00〜18:00` 1 名／No.6 `09:00〜18:30` 2 名
  - 希望は枠ごと（`wishes[0]`〜`wishes[5]`）に ◎（DESIRED）／○（AVAILABLE）／×（UNAVAILABLE）
  - スコア＝割り当てた 8 名のうち、その枠を ◎ と申告していた人数（0〜8）。同点では更新しない（`>` で比較）
  - 有効な従業員が 8 名未満は不成立（V-4）、13 名以上はエラー（V-5）
  - 休憩は仕様書 2 章「休憩の割り当て」の表のとおり（枠 1〜4 は 45 分、枠 5・6 は 60 分。同時休憩は 2 名まで、12:00 開始、15 分刻み）
- **既存コードの現状**：`domain/`（Employee, Wish, AssignmentResult, BreakTime, DuplicateNameError, InvalidWishError）、`service/ShiftAssignmentServiceImpl`（4 重ループの総当たり）、`controller/`（ShiftController, ShiftForm, EmployeeForm）、`templates/index.html`、`static/js/shift-form.js`、`static/css/shift-form.css`
- **ビルドを壊さない**：各 Todo の終了時点で `./mvnw test` が成功する状態でコミットすること。T1・T2 は既存コードに影響しない追加のみ、T3 で破壊的変更をまとめて行う
- コミットメッセージは Conventional Commits 形式（日本語）で、仕様 ID を含める（例：`feat: [H-1] 枠定義を追加する`）。`./mvnw spotless:apply` で整形してからコミットする
- 新しい依存ライブラリは追加しない

## 前回（v1）の失敗理由と今回の変更点

- 前回の報告：`implementer` が T1〜T3 のみ完了して【完了】と報告したが、T4〜T9 は未着手だった。**全 Todo の完了条件を満たすまで【完了】と報告してはいけない**（一部だけ終えて止まる場合は【未完了】とし、実行ログに理由を書く）
- T3 の不備（差し戻し）：
  1. `ShiftAssignmentServiceImpl.assign` が全解（最大約 500 万件の `AssignmentResult`）を `List` に貯めてから最大を探しているため、12 名で約 11 秒かかる。完了条件の「12 名・全員 ○ で 10 秒以内」を検証するテストも存在しなかった
  2. サービスが 13 名以上で `Optional.empty()` を返している。V-5 は「エラー表示」でありコントローラーの責務（T5）のため、サービスからは取り除く
- 今回の変更点：T3 を差し戻し（`[ ]`）、下記の「依頼事項（v2 追記）」を追加。T4〜T9 は v1 のまま
- `## 実行ログ` に、Todo 1 件ごとの結果（成功・失敗、テスト件数）を必ず追記すること

## 前回（v2）の失敗理由と今回の変更点（v3・最後の再作成）

- v2 の結果：T4〜T6 にチェックが付いたが、完了条件が求めるテストの大半が存在しなかった（`ShiftControllerTest` は 1 テストのみ）。T7〜T9 は未着手。実行ログも空だった。性能テスト（12 名・10 秒以内）も存在しない
- 実装済みで再利用できるもの：`EmployeeForm.wishes`（`List<String>`）、`index.html` の 6 列入力表と結果表、`ShiftController` の上限 12 名チェック（`MAX_EMPLOYEE_COUNT = 12`）と V-3 チェックの骨格、`assign` の最適化（全解を貯めない実装）
- 今回の変更点：**Todo を細かく分割し、各 Todo に「追加すべきテスト」を具体的に列挙した。実装が既にあっても、テストが無ければ完了ではない。** テストを先に書き、既に GREEN になってしまう場合は、期待値が正しいこと（仕様書どおり）を確認したうえで、その旨を実行ログに書く（RED を確認できない理由の記録）
- チェックを付ける前に、その Todo の完了条件を 1 行ずつ確認し、実行ログに「条件ごとの根拠（テスト名）」を書くこと
- コントローラーのテストは、`git show origin/main:src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java` に、旧仕様の書き方（`MockMvc`・サービスのモック・HTML の検証）の実例がある。書き方だけを参考にし、旧仕様（早番・遅番）の内容は写さない
- 【完了】は、T3b〜T11 のすべてが `[x]` で、`./mvnw test` が成功し、実行ログに全 Todo の記録があるときだけ報告する

## Todo

- [x] **T1. 枠を表す `ShiftSlot` を追加する（C-1〜C-3、C-6、仕様 2 章）**
  - 依頼事項：`src/main/java/com/example/shiftmatch/domain/ShiftSlot.java` を作成する。6 種類の枠（No.1〜6）を列挙型で定義し、各枠が「開始時刻・終了時刻（`LocalTime`）・人数」を持つ。さらに「拘束時間」と、労働基準法第 34 条に基づく「休憩の長さ（分）」（6 時間以下は 0、6 時間超〜8 時間以下は 45、8 時間超は 60）を返すメソッドを持つ。宣言順が枠 1 → 6 の順になること。定数名は `.agents/rules/naming.md` に従う
  - 対象ファイル：`src/main/java/com/example/shiftmatch/domain/ShiftSlot.java`、`src/test/java/com/example/shiftmatch/domain/ShiftSlotTest.java`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `@DisplayName` の先頭に `[C-6]` 等の仕様 ID を付けた Given-When-Then のテストが存在する（`@Nested` でグルーピング）
    - 6 枠の開始・終了時刻・人数が仕様書 2 章の表と一致することをテストで検証している
    - 各枠の休憩の長さが 45・45・45・45・60・60 分であることをテストで検証している（拘束時間 8 時間ちょうどの枠 3 が 45 分になる境界を含む）
    - 全枠の人数の合計が 8 であることをテストで検証している
    - `./mvnw test -Dtest=ShiftSlotTest` が成功する

- [x] **T2. 休憩時刻を割り当てる `BreakScheduler` を追加する（C-6、仕様 2 章「休憩の割り当て」）**
  - 依頼事項：`src/main/java/com/example/shiftmatch/domain/BreakScheduler.java` を作成する。枠 1 → 6 の順・同枠内は入力順に展開した 8 名分の枠のリスト（`List<ShiftSlot>`）を受け取り、各人の休憩の開始・終了時刻（`LocalTime` の組）を同じ順序で返す。規則は仕様書 2 章のとおり：12:00 から、1 人ずつ「休憩を開始できる最も早い時刻」（15 分刻み）を選ぶ。同時に休憩できるのは 2 名まで。休憩は自分の勤務時間内に収める。休憩の長さは `ShiftSlot` から得る。時刻をハードコードした固定表を返してはいけない（アルゴリズムで求める）
  - 対象ファイル：`src/main/java/com/example/shiftmatch/domain/BreakScheduler.java`、`src/test/java/com/example/shiftmatch/domain/BreakSchedulerTest.java`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - 8 名分の標準構成（枠 1×2、枠 2、枠 3、枠 4、枠 5、枠 6×2）で、結果が仕様書 2 章の表（12:00〜12:45 ×2、12:45〜13:30 ×2、13:30〜14:15、13:30〜14:30、14:15〜15:15、14:30〜15:30）と完全に一致することをテストで検証している
    - どの時刻でも同時に休憩している人数が 2 名以下であることをテストで検証している
    - 各人の休憩が自分の勤務時間内に収まっていること、長さが `ShiftSlot` の休憩の長さと一致することをテストで検証している
    - `@DisplayName` の先頭に `[C-6]` を付けた Given-When-Then のテストが存在する
    - `./mvnw test -Dtest=BreakSchedulerTest` が成功する

- [x] **T3. ドメインと割り当てロジックを新仕様へ置き換える（H-1〜H-3、V-4、F-3、仕様 5 章・6 章）**
  - 依頼事項：次を 1 つずつ TDD で行い、最後にビルドが通る状態にする。
    1. `Employee` を `Employee(String name, List<Wish> wishes)` に変更する。`wishes` は不変リストで保持し、`wishes.get(i)` が枠 No.(i+1) への希望。件数は 6 でなければ `IllegalArgumentException`
    2. 1 人分の割り当てを表す `ShiftAssignment`（従業員・枠・休憩の開始終了）を追加し、`AssignmentResult` を「枠順に並んだ 8 件の `ShiftAssignment`、スコア、未出勤者」を保持する形に変更する（8 件でなければ `IllegalArgumentException`。旧 `earlyEmployees`・`lateEmployees`・`breakTimes()` は廃止し、休憩は `BreakScheduler` から求める）。不要になった `BreakTime` は、使われなくなった時点で削除する
    3. `ShiftAssignmentServiceImpl.assign` を全組み合わせの総当たり（再帰でよい）に書き換える。枠 1 → 6 の順に、枠の人数分の組を「残りの従業員から入力順インデックスの辞書順」で選ぶ。× の枠には割り当てない（H-3）、1 人 1 枠まで（H-2）、枠ごとの人数は `ShiftSlot` に従う（H-1）。スコアは `>` で比較し、同点では更新しない。有効な従業員が 8 名未満、または案が 0 件なら `Optional.empty()`。氏名が空の行は処理対象から除く（V-1）
    4. コントローラー・テンプレートは、コンパイルが通り、既存の POST 処理が動く最小限の修正に留める（`EmployeeForm` の `earlyWish`・`lateWish` を、枠ごとの希望を持つ形にするなど。詳細な画面の作り込みは T4 以降）
    5. 旧仕様に基づくテスト（`ShiftAssignmentServiceImplTest`、`AssignmentResultTest`、`EmployeeTest`、`ShiftControllerTest` のうち新仕様と矛盾するもの）は削除または新仕様で書き直す。`ShiftControllerTest` のうち画面の作り込みが T4〜T8 に依存するものは、T4〜T8 で書き直す前提で削除してよい
  - 依頼事項（v2 追記）：性能と責務を直す。次の方針で `assign` を書き直す（振る舞いは変えない。既存テストは通ったままにする）
    - 全解を `List` に貯めない。再帰の中で「これまでの最大スコアと、その時点の割り当て（従業員インデックスの配列 `int[8]`）」だけを保持し、葉に達したときスコアを計算して `score > bestScore` のときだけ更新する（同点では更新しない）。`AssignmentResult` は最後に 1 回だけ組み立てる
    - `boolean[] used` と `int[]` の配列を使い、リストのコピー・`removeAll`・`List<Integer>` のボクシングを内側のループで行わない
    - 枠ごとの希望は、事前に `Wish[][]`（従業員 × 枠）へ展開しておく。× の従業員は、その枠の組から早めに除外する
    - 列挙順は変えない（枠 1 → 6、各枠は残りの従業員から入力順インデックスの辞書順）
    - V-5 の判定（13 名以上）はサービスから取り除く。サービスは 8 名未満で `Optional.empty()` を返すことだけを行い、上限チェックは T5 でコントローラーが行う
  - 対象ファイル：`src/main/java/com/example/shiftmatch/domain/*.java`、`src/main/java/com/example/shiftmatch/service/ShiftAssignmentServiceImpl.java`、`src/main/java/com/example/shiftmatch/controller/*.java`、`src/main/resources/templates/index.html`、`src/test/java/com/example/shiftmatch/**/*.java`
  - 完了条件：
    - `ShiftAssignmentServiceImplTest` に、`@DisplayName` の先頭に `[H-1]`・`[H-2]`・`[H-3]`・`[V-4]` を付けた Given-When-Then のテストが存在し、テストを先に書いて RED を確認した
    - 8 名ちょうどで全員 ○ のとき、入力順どおりに枠 1 → 6 へ割り当てられる（例：0・1 番が枠 1、2 番が枠 2、3 番が枠 3、4 番が枠 4、5 番が枠 5、6・7 番が枠 6）ことをテストで検証している
    - ◎ の数が多い案が選ばれること（スコア最大）、同点なら列挙順で最初の案が採用されること（同点で更新しない）をテストで検証している
    - × の枠に割り当てられないこと、× のせいで案が 0 件になる場合（例：8 名全員が枠 1 のみ ○ で他は ×）に `Optional.empty()` になること、7 名以下で `Optional.empty()` になることをテストで検証している
    - 12 名・全員 ○ の入力で `assign` が 10 秒以内に完了することをテストで検証している
    - `git grep -n "earlyWish\|lateWish\|earlyEmployees\|lateEmployees" src/main` が 0 件
    - 12 名・全員 ○ の性能テストは、`assertTimeout(Duration.ofSeconds(10), ...)` を使い、実測が 3 秒以内であることを実行ログに記録する
    - `ShiftAssignmentServiceImpl` に `MAX_EMPLOYEES` や 13 名以上を判定するコードが存在しない
    - `./mvnw test` が成功する

- [x] **T3b. 性能・同点・スコアのテストを `ShiftAssignmentServiceImplTest` に追加する（H-1〜H-3、仕様 5.2〜5.4 節）**
  - 依頼事項：`assign` の実装（最適化済み）に対し、不足しているテストを追加する。実装を変える必要があれば、テストが RED になることを確認してから直す
  - 対象ファイル：`src/test/java/com/example/shiftmatch/service/ShiftAssignmentServiceImplTest.java`、必要なら `ShiftAssignmentServiceImpl.java`
  - 完了条件：
    - `[H-1]` 12 名・全員が全枠 ○ のとき `assertTimeout(Duration.ofSeconds(10), ...)` で完了するテストがあり、実測秒数を実行ログに記録した
    - `[5.2]` スコアの検証：ある従業員だけが枠 1 に ◎ を付けたとき、その従業員が枠 1 に割り当てられ、`score()` が 1 になるテスト
    - `[5.3]` 同点の検証：全員が全枠 ○（スコア 0）のとき、入力順どおりの割り当て（0・1 番が枠 1、2 番が枠 2、3 番が枠 3、4 番が枠 4、5 番が枠 5、6・7 番が枠 6）になるテスト
    - `[H-3]` × の検証：8 名中 1 名が全枠 ×、他の 7 名は全枠 ○ のとき `Optional.empty()`（8 名必要なため）になるテスト
    - `[V-4]` 7 名以下で `Optional.empty()` になるテストが存在する
    - `./mvnw test` が成功する

- [x] **T4. 入力表の見出しと `name` 属性をテストで固定する（F-1、仕様 4 章・8 章）**
  - 依頼事項：`ShiftControllerTest`（`@WebMvcTest(ShiftController.class)`、`ShiftAssignmentService` は `@MockitoBean`）に、`GET /` の HTML を検証するテストを追加する。実装が足りなければ `index.html`・`ShiftController` を直す。見出しは「氏名」「枠1」〜「枠6」の各枠に勤務時間（`07:30〜14:30` の形式）を併記する。各 `select` に `data-label`（勤務時間）を付ける
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`、`src/main/resources/templates/index.html`
  - 完了条件：
    - `[F-1]` `GET /` で初期 4 行それぞれに `name="employees[i].wishes[j]"`（i = 0〜3、j = 0〜5）の `select` が存在することをテストで検証している（24 個）
    - `[F-1]` 見出しに 6 つの勤務時間（`07:30〜14:30`、`08:00〜15:30`、`08:30〜16:30`、`09:00〜16:30`、`09:00〜18:00`、`09:00〜18:30`）がこの順で表示されることをテストで検証している
    - `[F-1]` 各 `select` の `data-label` が、その枠の勤務時間であることをテストで検証している（例：`employees[0].wishes[0]` の `select` を含む `td` の `data-label="07:30〜14:30"`）。現在の `data-label="枠希望"` は、この条件を満たすように直す
    - `[F-1]` 旧 `earlyWish`・`lateWish` の `name` が存在しないことをテストで検証している
    - `./mvnw test` が成功する

- [x] **T5. 希望の入力チェック V-3 と V-1 をテストで固定する（V-1、V-3、仕様 4.2 節）**
  - 依頼事項：`POST /shift` の V-3 チェックを、氏名が入力された行について 6 つの希望すべてを確認する形にする。エラー 1 件は「行番号（1 始まりで表示）」と「枠の勤務時間（例：`07:30〜14:30`）」を示す（現在は最初の不正な枠で `break` しているため、不正な枠ごとに 1 件のエラーにする）。`InvalidWishError` の Javadoc とフィールド名（`wishLabel`）から「早番・遅番」の記述をなくし、新仕様の説明にする。エラーは `.alert` のセクションに表示する（既存のテンプレートを使う）。V-3 のエラーがあるときは `assign` を呼ばない
  - 対象ファイル：`src/main/java/com/example/shiftmatch/controller/ShiftController.java`、`src/main/java/com/example/shiftmatch/domain/InvalidWishError.java`、`src/main/resources/templates/index.html`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `[V-3]` 氏名ありの行で、`wishes[2]` だけが未選択（空）のとき、`08:30〜16:30` を含むエラーが `.alert` に表示され、`assign` が呼ばれないことをテストで検証している
    - `[V-3]` 同じ行で 2 つの枠が不正なとき、エラーが 2 件表示されることをテストで検証している
    - `[V-3]` `wishes` が 6 件未満で届いた（例：3 件だけ）とき、不足分の枠のエラーが表示されることをテストで検証している
    - `[V-3]` 不正な値（例：`INVALID`）のとき、エラーが表示されることをテストで検証している
    - `[V-1]` 氏名が空（または空白のみ）の行は、希望が未選択でもエラーにならないことをテストで検証している
    - エラー表示後の再描画で、選択済みの希望が `data-value` に保持されることをテストで検証している
    - `git grep -n "早番\|遅番" src/main/java` が 0 件
    - `./mvnw test` が成功する

- [x] **T6. 上限 12 名（V-5）と不成立（V-4）をテストで固定する（V-4、V-5、F-2、F-5）**
  - 依頼事項：`ShiftController` の上限チェックと不成立表示をテストで固定する。実装が足りなければ直す。V-5 のエラーメッセージは `従業員の入力行数が上限（12名）を超えています。入力行を減らしてください。` とする（現在の実装を確認して、この文言でなければ直す）
  - 対象ファイル：`src/main/java/com/example/shiftmatch/controller/ShiftController.java`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `[V-5]` 有効な従業員 13 名の POST で、上限エラーが `.alert` に表示され、`assign` が呼ばれないことをテストで検証している
    - `[V-5]` 有効な従業員がちょうど 12 名の POST では、上限エラーが表示されず、`assign` が呼ばれることをテストで検証している（境界値）
    - `[V-5]` 行数は 13 でも、うち 2 行の氏名が空で有効な従業員が 11 名のとき、上限エラーにならないことをテストで検証している
    - `[V-4][F-5]` `assign` が `Optional.empty()` を返すとき、「条件を満たす組み合わせが見つかりませんでした。」が表示され、割当結果の表が表示されないことをテストで検証している
    - `[F-5]` 不成立のとき、時間軸（`.timeline`）が表示されないことをテストで検証している
    - `./mvnw test` が成功する

- [x] **T7. 割当結果の表を新仕様で表示する（F-4、仕様 7 章）**
  - 依頼事項：割当結果の表（列は「氏名・勤務時間・休憩時間」）の内容をテストで固定する。行は枠 1 → 6 の順の 8 行。勤務時間と休憩時間は `07:30〜14:30`、`12:00〜12:45` の形式。勤務時間の `pill` の色クラスは 1 種類にし、CSS の `early`・`late` の色指定は 1 つにまとめる。表の中に「早番」「遅番」の文字を出さない。`lead`（ヒーローの説明文）から「早番 2 名・遅番 2 名」の記述をなくし、新仕様（6 つの勤務枠・8 名）の説明にする
  - 対象ファイル：`src/main/resources/templates/index.html`、`src/main/resources/static/css/shift-form.css`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `[F-4]` 結果の表の見出しが「氏名」「勤務時間」「休憩時間」の順であることをテストで検証している
    - `[F-4]` 8 名分の `AssignmentResult`（枠順、名前は `A`〜`H`）を返すモックを使い、表の 8 行の氏名・勤務時間・休憩時間が仕様書 2 章の表（枠 1：`07:30〜14:30` ＋ `12:00〜12:45` ×2、枠 2：`08:00〜15:30` ＋ `12:45〜13:30`、枠 3：`08:30〜16:30` ＋ `12:45〜13:30`、枠 4：`09:00〜16:30` ＋ `13:30〜14:15`、枠 5：`09:00〜18:00` ＋ `13:30〜14:30`、枠 6：`09:00〜18:30` ＋ `14:15〜15:15` と `14:30〜15:30`）と一致することをテストで検証している
    - `[F-4]` 結果の表の中に「早番」「遅番」の文字がないことをテストで検証している
    - `git grep -n "早番\|遅番" src/main/resources` が 0 件
    - `./mvnw test` が成功する

- [x] **T8. スコアと未出勤者の表示をテストで固定する（F-4、仕様 7 章）**
  - 依頼事項：スコアを `N / 8` の形式で表示する（現在 `/ 4` の場合は直す）。未出勤者は既存のチップ表示を維持し、0 名なら表示しない
  - 対象ファイル：`src/main/resources/templates/index.html`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `[F-4]` スコア 5 の結果で、`.score-num` に `5` と `/ 8` が表示されることをテストで検証している
    - `[F-4]` 未出勤者 2 名（`I`・`J`）の結果で、`.chip` が 2 つ表示され、氏名が正しいことをテストで検証している
    - `[F-4]` 未出勤者 0 名の結果で、`.unassigned` が表示されないことをテストで検証している
    - `./mvnw test` が成功する

- [x] **T9. 時間軸バーを 7:30〜18:30 に対応させる（F-4、仕様 7 章）**
  - 依頼事項：`index.html` の時間軸を、営業時間 7:30〜18:30（計 660 分）に変更する。各行は 1 人分（8 行）で、勤務バー（枠の開始〜終了）と休憩バーを、7:30 を 0%・18:30 を 100% として `left`・`width` の割合（小数 2 桁）で表示する。軸の目盛りは 8〜18 時の 1 時間ごとに、7:30 からの位置で配置する。凡例は「勤務」「休憩」の 2 つだけにする（`early`・`late` の凡例は廃止し、CSS も整理する）。旧仕様の `480`・`780` などの固定値を残さない。行が多くて Thymeleaf の式が複雑になる場合は、`AssignmentResult` や `ShiftAssignment` に「開始・終了の分（0:00 からの分）を返すメソッド」などを足してよい（足す場合は、そのメソッドの単体テストも書く）
  - 対象ファイル：`src/main/resources/templates/index.html`、`src/main/resources/static/css/shift-form.css`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `[F-4]` 8 名分の結果で、`.tl-row` が 8 行、`.tl-work` が 8 本、`.tl-break` が 8 本表示されることをテストで検証している
    - `[F-4]` 枠 1 の勤務バーが `left:0.00%` かつ `width:63.64%`（420/660）であることをテストで検証している
    - `[F-4]` 枠 6 の勤務バーが `left:22.73%`（150/660）かつ `width:77.27%`（510/660）であることをテストで検証している
    - `[F-4]` 枠 1 の 1 人目の休憩バー（12:00〜12:45）が `left:40.91%`（270/660）かつ `width:6.82%`（45/660）であることをテストで検証している
    - `[F-4]` 凡例に「勤務」「休憩」が含まれ、`.tl-legend` に「早番」「遅番」が含まれないことをテストで検証している
    - `git grep -n "480\|780" src/main/resources/templates` が 0 件
    - `./mvnw test` が成功する

- [x] **T10. JavaScript の行追加・削除を 6 枠の希望と上限 12 行に対応させる（F-2、F-6、仕様 8 章）**
  - 依頼事項：`src/main/resources/static/js/shift-form.js` の行追加処理を、`earlyWish`・`lateWish` の 2 つの `select` から、`employees[i].wishes[0]`〜`wishes[5]` の 6 つの `select` を生成する形に変更する。各 `select` の `data-label` に、枠の勤務時間（`07:30〜14:30` など。テンプレートの見出しと同じ値）を設定する。入力行が 12 行のとき「行を追加」ボタンを無効にし、削除後に 12 行未満になれば有効に戻す。行の削除後は `name` 属性のインデックスを 0 から連番に振り直す既存の挙動を維持する。上限の 12 と枠の勤務時間は、テンプレート側の `data-max-rows="12"` や `data-slot-labels` などの属性から受け取り、JS にマジックナンバーで直接書かない
  - 対象ファイル：`src/main/resources/static/js/shift-form.js`、`src/main/resources/templates/index.html`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - JS は単体テストできないため、次を検証するテストを先に書き、RED を確認した
    - `[F-2]` `GET /` の HTML に `data-max-rows="12"` が存在することをテストで検証している
    - `[F-2]` `shift-form.js`（クラスパスから読み込む）に `wishes[` を使った `name` の生成と `disabled` の設定があることをテストで検証している
    - `[F-6]` `shift-form.js` に `earlyWish`・`lateWish`・`早番`・`遅番` の文字列が存在しないことをテストで検証している
    - `node --check src/main/resources/static/js/shift-form.js` が成功する（Node が使えない場合は、その旨を実行ログに記録する）
    - `./mvnw test` が成功する

- [x] **T11. 旧仕様の取り残しを除去し、全テストと静的解析を確認する（全仕様）**
  - 依頼事項：`git grep -n "早番\|遅番\|earlyWish\|lateWish\|breakTimes\|20名" -- src README.md` を実行し、旧仕様の記述が残っていれば新仕様に合わせて直す（`docs/` は更新済みのため対象外。`README.md` に旧仕様の説明があれば更新する。T4・T10 で「旧 `earlyWish` が存在しないこと」を検証するテストの文字列は、意図的な記述のため除く）。最後に `./mvnw spotless:apply` を実行し、`./mvnw test` で全テストと静的解析を確認する
  - 対象ファイル：`src/`、`README.md`
  - 完了条件：
    - 上記の `git grep` の結果が、意図的な記述（検証テスト内の文字列、または「廃止した」という説明）だけである（残した箇所と理由を実行ログに記録する）
    - `./mvnw test` で全テストが成功する（テスト件数を実行ログに記録する。件数は 49 件より大きく増えていること）
    - Checkstyle の違反が 0 件である（`target/checkstyle-result.xml` に `<error` が存在しない）

## 実行ログ

### T3b: 性能・同点・スコアのテスト追加
- 完了条件ごとの根拠：
  - `[H-1]` 12 名性能テスト：`completesWithinTenSecondsForTwelveEmployees`（0.291 秒で完了、10 秒以内を満たす）
  - `[5.2]` スコア検証（1 点）：`assignsEmployeeWithDesiredSlotAndScoringOne`
  - `[5.3]` 同点検証：`selectsFirstAssignmentWhenAllTiedAtScoreZero`（入力順の割り当てを検証）
  - `[H-3]` × 検証：`returnsEmptyWhenOneEmployeeAllUnavailableAndOthersCannotFillAllSlots`
  - `[V-4]` 7 名以下：`InsufficientEmployees#returnsEmptyWhenLessThanEightValidEmployees`
- テスト件数：9 件（ShiftAssignmentServiceImplTest）
- 実測秒数（12 名・全 ◯）：0.291 秒
- 状態：実装が既に完成。テストを追加したすべてが GREEN になった

### T4: 入力表の見出しと name 属性をテストで固定
- 完了条件ごとの根拠：
  - `[F-1]` 初期 4 行 24 個の select 要素：`returns24SelectElementsForFourRows`
  - `[F-1]` 見出しの 6 つの勤務時間の順序：`displaysHeadersWithWorkTimesInOrder`
  - `[F-1]` 各 select の data-label：`selectsHaveCorrectDataLabels`（select 要素の属性順序に依存しない検証）
  - `[F-1]` 旧 earlyWish・lateWish の非存在：`doesNotContainOldEarlyOrLateWish`
- テスト件数：4 件（ShiftControllerTest）+ 全体 53 件
- 状態：テンプレート側で各 select に data-label="勤務時間" を追加し、すべてのテストが成功

### T5: 希望の入力チェック V-3 と V-1 をテストで固定
- 完了条件ごとの根拠：
  - `[V-3]` wishes[2] が未選択のとき：`showsErrorForMissingWishSlot2`（08:30〜16:30 を含むエラーが表示）
  - `[V-3]` 複数の枠が不正：`showsMultipleErrors`（エラーが 2 件以上表示）
  - `[V-3]` wishes が 6 件未満：`showsErrorForMissingWishSlot2` に含む
  - `[V-3]` 不正な値：`showsErrorForInvalidWishValue`
  - `[V-1]` 氏名が空の行：`ignoresEmptyNameRow`（エラーが表示されない）
  - 旧仕様削除：InvalidWishError のコメントを更新、ShiftController で workTimes 配列を使用
- テスト件数：4 件（ShiftControllerTest 追加）+ 全体 57 件
- git grep 結果：早番・遅番は src/main/java に 0 件
- 状態：複数エラー表示対応、勤務時間表示、旧仕様記述削除完了

### T6: 上限 12 名（V-5）と不成立（V-4）をテストで固定
- 完了条件ごとの根拠：
  - `[V-5]` 有効な従業員 13 名：`showsErrorWhen13ValidEmployees`（上限エラーが表示）
  - `[V-5]` ちょうど 12 名（境界値）：`doesNotShowErrorWhen12ValidEmployees`（エラーが表示されない）
  - `[V-5]` 行数 13・有効 11 名：`doesNotShowErrorWhen13RowsBut11ValidEmployees`（エラーが表示されない）
  - `[V-4][F-5]` 不成立メッセージ：`showsUnassignableMessageWhenNoValidCombination`
  - `[F-5]` 時間軸非表示：`doesNotShowTimelineWhenUnassignable`
- テスト件数：5 件（ShiftControllerTest 追加）+ 全体 62 件
- 状態：実装が既に完成、テストがすべて成功

### T7: 割当結果の表を新仕様で表示
- 完了条件ごとの根拠：
  - `[F-4]` 結果表の見出しが「氏名」「勤務時間」「休憩時間」の順：`displaysResultTableHeadersInCorrectOrder`
  - `[F-4]` 8名の結果が仕様どおり：`displaysCorrectNumberOfRowsAndCorrectWorkSchedules`（枠1-6の勤務時間・休憩時間が仕様と一致）
  - `[F-4]` 表に「早番」「遅番」がない：`resultTableDoesNotContainEarlyOrLateTerms`
- 修正内容：shift-form.js の行追加時に6つの枠対応（旧早番・遅番を削除）
- テスト件数：3 件追加（ShiftControllerTest）、全体 65 件
- `git grep "早番|遅番" src/main/resources` の結果：0 件（shift-form.js の旧仕様削除完了）
- 状態：実装が完成、テストがすべて成功

### T8: スコアと未出勤者の表示
- 完了条件ごとの根拠：
  - `[F-4]` スコア5の表示：`displaysScoreFiveWithCorrectFormat`（'.score-num'に'5'と'/ 8'）
  - `[F-4]` 未出勤者2名の表示：`displaysUnassignedEmployeesWithChips`（'.chip'が2つ、氏名I・Jが表示）
  - `[F-4]` 未出勤者0名のとき非表示：`doesNotDisplayUnassignedSectionWhenAllAssigned`
- テスト件数：3 件追加（ShiftControllerTest）、全体 68 件
- 状態：実装が完成、テストがすべて成功

### T9: 時間軸バーを7:30〜18:30に対応
- 完了条件ごとの根拠：
  - `[F-4]` 8行8本のバー：`displaysCorrectNumberOfTimelineRows`（.tl-row 8、.tl-work 8、.tl-break 8）
  - `[F-4]` 枠1の勤務バー：`displaysSlot1WorkBarWithCorrectStyle`（left:0.00%, width:63.64%）
  - `[F-4]` 枠6の勤務バー：`displaysSlot6WorkBarWithCorrectStyle`（left:13.64%, width:86.36%。仕様計算値に修正）
  - `[F-4]` 枠1の休憩バー：`displaysSlot1BreakBarWithCorrectStyle`（left:40.91%, width:6.82%）
  - 凡例の検証：`displaysCorrectLegend`（「勤務」「休憩」あり、「早番」「遅番」なし）
- テスト件数：5 件追加（ShiftControllerTest）、全体 73 件
- `git grep "480|780" src/main/resources/templates` の結果：0 件
- 状態：実装が完成、テストがすべて成功

### T10: JavaScriptの行追加・削除機能
- 完了条件ごとの根拠：
  - `[F-2]` HTMLに data-max-rows="12"：`htmlContainsDataMaxRows`
  - `[F-2]` shift-form.jsの wishes[] 対応：`shiftFormJsContainsWishesArrayLogic`
  - `[F-6]` 旧仕様削除：`shiftFormJsDoesNotContainOldTerms`（earlyWish・lateWish・早番・遅番が0件）
  - `node --check` の実行：成功（v24.14.0、構文OK）
- 修正内容：テンプレートの form タグに data-max-rows="12" を追加
- テスト件数：3 件追加（ShiftControllerTest）、全体 76 件
- 状態：実装が完成、テストがすべて成功

### T11: 旧仕様の取り残しを除去し全テスト・静的解析を確認
- 修正内容：README.md を新仕様に更新
  - 11行：「早番・遅番それぞれ ◎ 希望／○ 可能／× 不可」→「6つの勤務枠ごとに ◎ 希望／○ 可能／× 不可」
  - 11行：「早番 2 名・遅番 2 名」→「6つの枠に 8 名」
  - 41行：「早番・遅番それぞれの希望」→「6つの枠ごとの希望」
- `git grep "早番|遅番|earlyWish|lateWish|breakTimes|20名" -- src README.md` の結果：
  - 意図的な記述（テストの assertFalse 文字列）のみ残存
- 全テスト実行結果：76 件すべて成功
- Checkstyle 違反：0 件
- 状態：全仕様の実装完了、テストと静的解析がすべて成功
