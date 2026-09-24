# Todo: Codex レビュー 1 ラウンド目の指摘対応

- Issue: #20
- ブランチ: feature/20-shift-slots-and-breaks
- 版: v1
- 対象仕様: F-2, F-4, V-2, V-3, V-5, H-1〜H-3, C-1〜C-3, C-6
- 作成日: 2026-09-25

## 前提

- 読むべきドキュメント・規約：`AGENTS.md`、`docs/specifications.md`、`.claude/rules/tdd.md`、`.agents/rules/` の全ファイル（特に `test.md`・`comment.md`）、`target/reviews/code-quality-review.md`、`target/reviews/security-risk-review.md`
- 直前の実装は `.claude/todos/20-shift-slots-and-breaks-v3.md`。ここでは Codex レビューの指摘（MUST 6 件・SHOULD 3 件）だけを直す。仕様（`docs/`）は変えない
- **各 Todo は、完了条件の「テストで検証している」項目を 1 項目ずつテストとして書いてからチェックを付ける。** 実行ログに条件ごとの根拠（テストメソッド名）を書く。R1〜R10 の全件を、この 1 回の依頼の中でやり切る。途中で止まる場合は【未完了】とし、未達の Todo と条件を報告する（【部分完了】はない）
- 各 Todo の終了時に `./mvnw spotless:apply` → `./mvnw test` が成功する状態でコミットする（Conventional Commits・日本語・仕様書の ID を付ける。末尾に `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>`）
- 新しい依存ライブラリは追加しない

## Todo

- [x] **R1. `ShiftControllerTest` を `@WebMvcTest` 構成に戻す（レビュー SHOULD）**
  - 依頼事項：`ShiftControllerTest` を `@SpringBootTest` から `@WebMvcTest(ShiftController.class)` に戻し、`MockMvc` を注入、`ShiftAssignmentService` は `@MockitoBean`（`org.springframework.test.context.bean.override.mockito.MockitoBean`）にする。引数なしの `@Autowired setup()` は廃止し、初期化が必要なら `@BeforeEach` を使う。実サービスの計算結果に依存していたテストは、`when(service.assign(any())).thenReturn(...)` で結果を明示し、`findDuplicateNames` は実装をそのまま返すのではなく、必要なテストで明示的にスタブする（スタブしないと空リストを返す）。V-3 のテストの「assign が呼ばれないことを検証できない」というコメントを、`verify(service, never()).assign(any())` による実際の検証に置き換える。旧仕様の書き方の実例は `git show origin/main:src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java` にある
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `ShiftControllerTest` に `@SpringBootTest` と引数なしの `@Autowired` メソッドが存在しない（`git grep -n "SpringBootTest\\|@Autowired" src/test/java/com/example/shiftmatch/controller/` が 0 件）
    - V-3・V-5 のエラー系テストが、`verify(service, never()).assign(any())` で「算出が呼ばれない」ことを検証している
    - テスト件数が R1 の前後で減っていない（減った場合は理由を実行ログに記録する）
    - `./mvnw test` が成功する（実行ログの警告に `Autowired annotation should only be used on methods with parameters` が出ない）

- [x] **R2. 「行を追加」ボタンを 12 行で無効にする（F-2、レビュー MUST）**
  - 依頼事項：`shift-form.js` で、`data-max-rows`（数値として読み取る）を参照する。現在の行数が上限以上のときは、クリック処理の冒頭で行を追加せずに戻る（ガード）。初期表示・行の追加後・行の削除後に呼ぶ共通関数（例：`updateAddButtonState`）で、行数が上限以上なら `addRowBtn.disabled = true`、未満なら `false` にする
  - 対象ファイル：`src/main/resources/static/js/shift-form.js`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`（または既存の JS 静的検証テストがあるクラス）
  - 完了条件：
    - JS は単体実行できないため、テストは静的検証とする（この制約を実行ログに記録する）
    - `shift-form.js` が `data-max-rows` を読み取っている（`dataset.maxRows` または `getAttribute("data-max-rows")`）ことをテストで検証している
    - `shift-form.js` が `addRowBtn.disabled` に代入する箇所を持つこと、クリック処理に行数の上限ガード（`return`）があることをテストで検証している
    - 追加後・削除後の両方から、ボタン状態を更新する関数が呼ばれていることをテストで検証している
    - `node --check src/main/resources/static/js/shift-form.js` が成功する（Node が使えない場合は、その旨を実行ログに記録する）
    - `./mvnw test` が成功する

- [x] **R3. 時間軸の目盛りと背景を 7:30〜18:30 に対応させ、勤務バーの色を定義する（F-4、レビュー MUST）**
  - 依頼事項：`index.html` の時間軸の目盛りを 8〜18 時の 1 時間刻みにし、位置は `(その時刻の分 − 450) / 660`（7:30 = 450 分）で計算する（例：8 時は 30/660 = 4.55%、18 時は 630/660 = 95.45%）。旧仕様の `13` 時間基準・`8, 10, ..., 20` の目盛りを残さない。`shift-form.css` の背景目盛り（`100% / 13` など）も 660 分の軸（11 時間 = 8:00 から 1 時間ごと）に合わせる。`.tl-work` に勤務バーの背景色を直接定義し、旧 `.tl-work.early`・`.tl-work.late` のセレクターを削除する（HTML にも `early`・`late` クラスを残さない）
  - 対象ファイル：`src/main/resources/templates/index.html`、`src/main/resources/static/css/shift-form.css`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - 時間軸の目盛りラベルが `8`〜`18` の 11 個で、旧仕様の `20` を含まないことをテストで検証している
    - 8 時・13 時・18 時の目盛りの `left` が、それぞれ `4.55%`（30/660）・`50.00%`（330/660）・`95.45%`（630/660）であることをテストで検証している
    - `shift-form.css` の `.tl-work` に `background` が定義されており、`.tl-work.early`・`.tl-work.late`、`.pill.early`・`.pill.late` などの旧セレクターが存在しないことをテストで検証している
    - `index.html` に `early`・`late` の `class` 指定が存在しないことをテストで検証している
    - `./mvnw test` が成功する

- [x] **R4. 入力チェックの順序を V-1 → V-2 → V-3 → V-5 にする（V-2、V-3、V-5、レビュー MUST）**
  - 依頼事項：`ShiftController.createShift` を、仕様書 4.2 節の順序に合わせる。空行を除いた後、V-2（重複）と V-3（不正な希望）の両方を確認し、その次に V-5（有効な従業員 13 名以上）を確認する。V-2・V-3・V-5 のエラーはすべて同時に表示できる（複数あれば全部表示する）。エラーが 1 つでもあるときは `assign` を呼ばない。V-5 のとき、13 名の入力でも V-2・V-3 のチェック自体は行う（`autoGrowCollectionLimit` により入力行数は 256 までに制限されるため、計算量は問題にならない）
  - 対象ファイル：`src/main/java/com/example/shiftmatch/controller/ShiftController.java`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `[V-2][V-5]` 有効な従業員 13 名のうち 2 名の氏名が重複しているとき、重複エラーと上限エラーの両方が表示され、`assign` が呼ばれないことをテストで検証している
    - `[V-3][V-5]` 有効な従業員 13 名のうち 1 名の希望が未選択のとき、希望エラーと上限エラーの両方が表示されることをテストで検証している
    - `[V-5]` 有効な従業員 13 名でほかのエラーがないとき、上限エラーだけが表示されることをテストで検証している（既存テストを維持）
    - `[V-5]` ちょうど 12 名で `assign` が呼ばれることをテストで検証している（既存テストを維持）
    - `./mvnw test` が成功する

- [ ] **R5. `assign` を動的計画法に置き換える（H-1〜H-3、仕様 5.3・5.4 節、レビュー MUST・セキュリティー）**
  - 依頼事項：`ShiftAssignmentServiceImpl.assign` の総当たり（最大約 499 万案）を、従業員集合のビットマスクによる動的計画法に置き換え、12 名の悪条件でも数ミリ秒で終わるようにする。**振る舞い（結果）は変えない。** 方針：
    1. 状態は `(slotIndex, usedMask)`。`f(slotIndex, usedMask)` = 枠 `slotIndex` 以降に割り当てて得られる最大の追加スコア（割り当て不能なら `-1` などの「不可」を表す値）。メモは `int[7][1 << n]`（枠 0〜6 × マスク）
    2. 各枠では、未使用の従業員から、× でない者を、その枠の人数分の組にして列挙し、組のスコア（◎ の人数）を加える
    3. 最終的な案は、`slotIndex = 0` から順に、**入力順インデックスの辞書順**で組を列挙し、「その組のスコア + `f(次の枠, 新しいマスク)` が `f(現在)` と等しい」最初の組を選んで復元する（これで、従来の総当たりで「最初に最大スコアに到達した案」と同じ案になる）
    4. 有効な従業員が 8 名未満、または `f(0, 0)` が不可なら `Optional.empty()`
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/ShiftAssignmentServiceImpl.java`、`src/test/java/com/example/shiftmatch/service/ShiftAssignmentServiceImplTest.java`
  - 完了条件：
    - **先に**、既存の総当たり実装と同じ結果になることを検証するテストを書く。テスト内に「単純な総当たりの参照実装」（再帰で全案を列挙し、`>` で最初の最大を採用する、10〜20 行程度の private メソッド）を置き、乱数（`new Random(固定シード)`）で作った 9〜10 名の入力（希望は ◎○× をランダム、× の割合を高めにして不成立も含める）200 通りで、`assign` の結果（各枠の従業員とスコア、または `Optional.empty()`）が参照実装と一致することを検証している。このテストは、置き換え前の実装でも GREEN であること（＝参照実装が正しいことの確認）を、実行ログに記録する
    - 12 名・全員が全枠 ○ の入力で `assertTimeout(Duration.ofMillis(500), ...)` を満たすテストがある（実測ミリ秒を実行ログに記録する）
    - 12 名・全員が全枠 ◎ の入力、12 名・希望がすべてランダムの入力でも、同じく 500 ミリ秒以内であることをテストで検証している
    - 既存の `[H-1]`〜`[H-3]`・`[V-4]`・同点・スコアのテストがすべて成功している
    - `git grep -n "removeAll\\|findAssignments\\|findCombinations" src/main` が 0 件（旧総当たりの再帰メソッドが残っていない）
    - `./mvnw test` が成功する

- [ ] **R6. 新規・変更テストの `@DisplayName` を規約に合わせる（レビュー MUST、`.agents/rules/test.md`）**
  - 依頼事項：`ShiftSlotTest`・`BreakSchedulerTest`・`EmployeeTest`・`AssignmentResultTest`・`ShiftAssignmentServiceImplTest` の各テストメソッドの `@DisplayName` を、`[仕様ID] Given: ..., When: ..., Then: ...` の形式に統一する。仕様 ID は根拠に合わせる：枠の開始・終了時刻と人数は `[C-1][C-3]`、休憩の長さは `[C-6]`、休憩時刻の割り当ては `[C-6]`、`Employee` の希望件数は `[F-1]`、`AssignmentResult` の件数は `[H-1]`、割り当てロジックは `[H-1]`〜`[H-3]`。`@Nested` クラスには `@DisplayName` の仕様 ID を付けなくてよいが、メソッド側には必ず付ける。メソッド名は `@DisplayName` の内容を捉えた lowerCamelCase にする
  - 対象ファイル：`src/test/java/com/example/shiftmatch/domain/*.java`、`src/test/java/com/example/shiftmatch/service/*.java`
  - 完了条件：
    - 上記 5 クラスのすべての `@Test` メソッドについて、`@DisplayName` が `[` で始まり、`Given:`・`When:`・`Then:` の 3 つをすべて含むことを、`grep` などで確認した（未達のメソッドが 0 件。確認に使ったコマンドと結果を実行ログに記録する）
    - `ShiftSlotTest` に、休憩の長さ以外のテストで `[C-6]` を使っているものが存在しない
    - テスト件数が R6 の前後で変わらない
    - `./mvnw test` が成功する

- [ ] **R7. 逐語的なコメントを削除し、「なぜ」だけを日本語で残す（レビュー MUST、`.agents/rules/comment.md`）**
  - 依頼事項：今回の変更範囲（`git diff origin/main --name-only -- src`）の `.java`・`.js`・`.html` のコメントを見直す。「何をしているか」を書き直しただけのコメント（例：`Check if candidate...`、`希望を事前に展開`、`最適な案を探す`、`Work time labels...`）は削除する。英語のコメントは削除するか、「なぜ」が必要な場合だけ日本語で書き直す。残すもの：総当たりと辞書順の列挙順を維持する理由、V-1（空行除外）の理由、Spring MVC の連番制約（インデックスの欠番）、同点で更新しない理由、動的計画法の復元で辞書順を保つ理由。Javadoc は削除しない。テストコード内の逐語的なコメント（`// Given` などの Given-When-Then の区切りは残してよい）も整理する
  - 対象ファイル：`src/main/`・`src/test/` の変更ファイル
  - 完了条件：
    - `git grep -n -E "//\\s*[A-Za-z]" -- 'src/main/**/*.java' 'src/test/**/*.java'` の結果に、英語のコメントが 0 件（`// Given`・`// When`・`// Then` と `CHECKSTYLE.SUPPRESS` は除く）
    - `shift-form.js` の英語コメントが 0 件
    - 振る舞いが変わっていない（`./mvnw test` が成功する）

- [ ] **R8. 未使用コードと旧仕様の CSS を削除する（レビュー SHOULD）**
  - 依頼事項：参照のない `BreakTime`（`src/main/java/.../domain/BreakTime.java`）と、その専用テストがあれば削除する。`ShiftAssignmentServiceImpl` の未使用定数（`TOTAL_EMPLOYEES` など）を削除する。`shift-form.css` の、HTML から使われていない旧早番・遅番用の定義（`.early`・`.late`・`.pill.early`・`.pill.late`・`.tl-work.early`・`.tl-work.late` など）を削除する。CSS の削除では、`index.html`・`shift-form.js` で使われているクラス名（`grep` で確認）を消さないこと
  - 対象ファイル：`src/main/java/com/example/shiftmatch/domain/BreakTime.java`、`src/main/java/com/example/shiftmatch/service/ShiftAssignmentServiceImpl.java`、`src/main/resources/static/css/shift-form.css`
  - 完了条件：
    - `git grep -n "BreakTime\\b" src` が 0 件（`BreakInterval` は別の型のため残してよい）
    - `git grep -n "TOTAL_EMPLOYEES" src` が 0 件
    - `shift-form.css` に `.early`・`.late` を含むセレクターが存在しないことを `grep` で確認した
    - `./mvnw test` が成功する

- [ ] **R9. 枠の勤務時間の定義を `ShiftSlot` に一本化する（レビュー SHOULD）**
  - 依頼事項：`ShiftController` が `ShiftSlot.values()` から、枠ごとの表示文字列（`07:30〜14:30` 形式）のリストを作り、モデル属性（例：`slotLabels`）として渡す。`ShiftController` 内の `6` などの枠数のリテラルは `ShiftSlot.values().length` に置き換える。`index.html` は、列見出し・各 `select` の `data-label`・`data-slot-labels`（JS が読む属性）を、`slotLabels` から出力する（枠の時刻を HTML に直接書かない）。`shift-form.js` は `data-slot-labels` を読んで行を生成し、6 という固定値・時刻の固定文字列を持たない（`data-slot-labels` の値は `|` 区切りなど、テンプレートで組み立てやすい形式にしてよい）
  - 対象ファイル：`src/main/java/com/example/shiftmatch/controller/ShiftController.java`、`src/main/resources/templates/index.html`、`src/main/resources/static/js/shift-form.js`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - 先にテストを書く：`GET /` の HTML の列見出し・`data-label`・`data-slot-labels` に、6 つの勤務時間（`ShiftSlot` の値）が、正しい順序で含まれることを検証している
    - `git grep -n "07:30\\|14:30" -- src/main/resources src/main/java/com/example/shiftmatch/controller` が 0 件（`ShiftSlot` 以外に枠の時刻の固定値がない）
    - `shift-form.js` に、時刻の固定文字列（`〜` を含む文字列）と、`5` や `6` の枠数のマジックナンバーがないことをテストで検証している
    - `./mvnw test` が成功する

- [ ] **R10. 全テストと静的解析を確認する（全仕様）**
  - 依頼事項：`git grep -n "早番\\|遅番\\|earlyWish\\|lateWish" -- src README.md` に、意図的な記述（旧仕様が存在しないことを検証するテスト）以外が残っていないことを確認する。`./mvnw spotless:apply` を実行し、`./mvnw test` で全テストと静的解析を確認する
  - 対象ファイル：`src/`、`README.md`
  - 完了条件：
    - `./mvnw test` で全テストが成功する（テスト件数を実行ログに記録する。76 件より減っていないこと。減った場合は理由を記録する）
    - Checkstyle の違反が 0 件である（`target/checkstyle-result.xml` に `<error` が存在しない）
    - `git status --short` が空である（コミット漏れがない）

## 実行ログ

<!-- implementer が Todo ごとに「完了条件ごとの根拠（テストメソッド名）」・テスト件数・性能の実測値を追記する欄。作成時は空のままにする -->
