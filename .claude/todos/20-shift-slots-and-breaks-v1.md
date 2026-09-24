# Todo: 割り当て枠を6種類8名へ変更し、休憩の自動割り当てと上限12名を実装する

- Issue: #20
- ブランチ: feature/20-shift-slots-and-breaks
- 版: v1
- 対象仕様: F-1, F-3, F-4, F-5, F-2, F-6, V-3, V-4, V-5, H-1, H-2, H-3, C-1〜C-7
- 作成日: 2026-09-24

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
  - 対象ファイル：`src/main/java/com/example/shiftmatch/domain/*.java`、`src/main/java/com/example/shiftmatch/service/ShiftAssignmentServiceImpl.java`、`src/main/java/com/example/shiftmatch/controller/*.java`、`src/main/resources/templates/index.html`、`src/test/java/com/example/shiftmatch/**/*.java`
  - 完了条件：
    - `ShiftAssignmentServiceImplTest` に、`@DisplayName` の先頭に `[H-1]`・`[H-2]`・`[H-3]`・`[V-4]` を付けた Given-When-Then のテストが存在し、テストを先に書いて RED を確認した
    - 8 名ちょうどで全員 ○ のとき、入力順どおりに枠 1 → 6 へ割り当てられる（例：0・1 番が枠 1、2 番が枠 2、3 番が枠 3、4 番が枠 4、5 番が枠 5、6・7 番が枠 6）ことをテストで検証している
    - ◎ の数が多い案が選ばれること（スコア最大）、同点なら列挙順で最初の案が採用されること（同点で更新しない）をテストで検証している
    - × の枠に割り当てられないこと、× のせいで案が 0 件になる場合（例：8 名全員が枠 1 のみ ○ で他は ×）に `Optional.empty()` になること、7 名以下で `Optional.empty()` になることをテストで検証している
    - 12 名・全員 ○ の入力で `assign` が 10 秒以内に完了することをテストで検証している
    - `git grep -n "earlyWish\|lateWish\|earlyEmployees\|lateEmployees" src/main` が 0 件
    - `./mvnw test` が成功する

- [ ] **T4. 入力表を枠ごと 6 列の希望入力にする（F-1、V-3、仕様 4 章・8 章）**
  - 依頼事項：`EmployeeForm` の希望を `List<String> wishes`（初期状態で 6 件の `null`）にし、フォーム名は `employees[i].wishes[j]`（j = 0〜5）とする。`ShiftController` の V-3 チェックは、氏名が入力された行について 6 つの希望それぞれが `DESIRED`／`AVAILABLE`／`UNAVAILABLE` のいずれかであることを確認し、不正な場合は `InvalidWishError` に「行番号」と「その枠の勤務時間（例：`07:30〜14:30`）の希望」を持たせて表示する。リクエストに `wishes` が 6 件未満で届いた場合も、不足分を未選択として V-3 のエラーにする。`index.html` の入力表は「氏名・枠 1〜6 の希望・削除」の列とし、枠の列見出しに勤務時間（`07:30〜14:30` の形式）を表示する。`th:field="*{employees[__${stat.index}__].wishes[__${j}__]}"` で 6 個の `<select>` を繰り返し出力し、各 `select` に `data-label`（勤務時間）と `data-value` を付ける
  - 対象ファイル：`src/main/java/com/example/shiftmatch/controller/EmployeeForm.java`、`ShiftController.java`、`src/main/java/com/example/shiftmatch/domain/InvalidWishError.java`、`src/main/resources/templates/index.html`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - テストを先に書き、RED を確認した
    - `GET /` の HTML に `name="employees[0].wishes[0]"` 〜 `name="employees[0].wishes[5]"` の `select` が存在し、旧 `earlyWish`・`lateWish` が存在しないことをテストで検証している
    - 表の見出しに 6 つの勤務時間（`07:30〜14:30`、`08:00〜15:30`、`08:30〜16:30`、`09:00〜16:30`、`09:00〜18:00`、`09:00〜18:30`）が表示されることをテストで検証している
    - 氏名が入力された行で希望が未選択・不正のとき、該当行と枠を示すエラーが表示され、`assign` が呼ばれないことをテストで検証している（`[V-3]`）
    - 氏名が空の行はエラーにならないことをテストで検証している（`[V-1]`）
    - エラー表示後の再描画で、選択済みの希望が `data-value` に保持されることをテストで検証している
    - `./mvnw test` が成功する

- [ ] **T5. 従業員数の上限 12 名（V-5）と不成立の表示を実装する（V-4、V-5、F-2、F-5、仕様 4 章・6 章）**
  - 依頼事項：`ShiftController` の上限を 20 から 12 に変更する（定数名・エラーメッセージも 12 名に合わせる。メッセージ例：`従業員の入力行数が上限（12名）を超えています。入力行を減らしてください。`）。有効な従業員（氏名が空でない行）が 13 名以上のときはエラーを表示し、`assign` を呼ばない（V-5）。有効な従業員が 8 名未満のときは、エラーではなく既存の「条件を満たす組み合わせが見つかりませんでした。」（不成立）を表示する（V-4）。チェックの順序は V-1 → V-5 とする
  - 対象ファイル：`src/main/java/com/example/shiftmatch/controller/ShiftController.java`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - テストを先に書き、RED を確認した
    - 有効な従業員 13 名の POST で上限エラー（`.alert`）が表示され、`assign` が呼ばれないことをテストで検証している（`[V-5]`）
    - 有効な従業員がちょうど 12 名の POST ではエラーにならず、`assign` が呼ばれることをテストで検証している（境界値）
    - 氏名が空の行が混ざり、行数は 13 でも有効な従業員が 12 名以下ならエラーにならないことをテストで検証している
    - 有効な従業員が 7 名以下のとき（`assign` が空を返す）、不成立メッセージが表示され、エラー表示にならないことを、実サービスを使ったテストで検証している（`[V-4]`）
    - `./mvnw test` が成功する

- [ ] **T6. 結果の表とスコアを新仕様で表示する（F-4、仕様 7 章）**
  - 依頼事項：`index.html` の割当結果を、列「氏名・勤務時間・休憩時間」の表にする。行は枠 1 → 6 の順（8 行）で、勤務時間は `07:30〜14:30`、休憩時間は `12:00〜12:45` の形式（24 時間表記・前 0 埋め）で表示する。スコアの表示は `N / 8` とし、ラベルは既存のもの（`スコア（◎ が反映された人数）`）を維持する。未出勤者は既存のチップ表示を維持する（0 名なら表示しない）。「早番」「遅番」の文字と、それに対応する `early`・`late` クラスは表から取り除き、勤務時間の `pill` は 1 種類のクラスにする（`static/css/shift-form.css` の対応する `early`・`late` の色指定も 1 つにまとめる）。ヒーローの説明文（`lead`）は新仕様に合わせて書き換える（例：「従業員の希望を入力すると、6 つの勤務枠（8 名）の最適な割り当て案を提案します。」）
  - 対象ファイル：`src/main/resources/templates/index.html`、`src/main/resources/static/css/shift-form.css`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - テストを先に書き、RED を確認した
    - 結果の表の見出しが「氏名」「勤務時間」「休憩時間」の順であることをテストで検証している
    - 8 名分の割り当て結果を与えたとき、表の 8 行の氏名・勤務時間・休憩時間が枠 1 → 6 の順に期待値どおり表示されることをテストで検証している（`[F-4]`）
    - スコアが `N / 8` の形式で表示されることをテストで検証している
    - 未出勤者がチップで表示され、0 名なら表示されないことをテストで検証している
    - 結果の表の中に「早番」「遅番」の文字がないことをテストで検証している
    - `./mvnw test` が成功する

- [ ] **T7. 時間軸バーを 7:30〜18:30 に対応させる（F-4、仕様 7 章）**
  - 依頼事項：`index.html` の時間軸バーを、営業時間 7:30〜18:30（計 660 分）の軸に変更する。各行は 1 人分で、勤務バー（枠の開始〜終了）と休憩バーを、7:30 を 0%・18:30 を 100% として `left`・`width` の割合（小数 2 桁）で表示する。軸の目盛りは 8〜18 時の 1 時間ごととし、7:30 からの位置で配置する。凡例は「勤務」「休憩」の 2 つだけにし、「早番」「遅番」は表示しない。旧仕様の 480 分・780 分などの固定値は残さない
  - 対象ファイル：`src/main/resources/templates/index.html`、`src/main/resources/static/css/shift-form.css`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - テストを先に書き、RED を確認した
    - 8 名分の結果で、時間軸の行が 8 行、勤務バーが 8 本、休憩バーが 8 本表示されることをテストで検証している
    - 枠 1（7:30〜14:30）の勤務バーが `left:0.00%` で `width:` が 7 時間ぶん（420/660 = 63.64%）になることなど、少なくとも 2 種類の枠の `left`・`width` の値を厳密にテストで検証している
    - 枠 1 の 1 人目の休憩バー（12:00〜12:45）の `left`（270/660 = 40.91%）と `width`（45/660 = 6.82%）をテストで検証している
    - 凡例に「勤務」「休憩」が含まれ、「早番」「遅番」が含まれないことをテストで検証している
    - 不成立のとき、時間軸が表示されないことをテストで検証している
    - `./mvnw test` が成功する

- [ ] **T8. JavaScript の行追加・削除を 6 枠の希望と上限 12 行に対応させる（F-2、F-6、仕様 8 章）**
  - 依頼事項：`src/main/resources/static/js/shift-form.js` の行追加処理を、`earlyWish`・`lateWish` の 2 つの `select` から、`employees[i].wishes[0]`〜`wishes[5]` の 6 つの `select` を生成する形に変更する。各 `select` の `data-label` には枠の勤務時間（`07:30〜14:30` など）を設定する。入力行が 12 行のとき「行を追加」ボタンを無効にし、削除後に 12 行未満になれば有効に戻す。行の削除後は `name` 属性のインデックスを 0 から連番に振り直す既存の挙動を維持する（`wishes[j]` の `j` は変えない）。上限の 12 は、テンプレート側の `data-max-rows` 属性などから受け取り、JS にマジックナンバーで直接書かない
  - 対象ファイル：`src/main/resources/static/js/shift-form.js`、`src/main/resources/templates/index.html`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`（または `src/test/java/com/example/shiftmatch/StaticResourceTest.java`）
  - 完了条件：
    - JS は単体テストできないため、静的リソースの内容と `GET /` の HTML を検証するテストを先に書き、RED を確認した
    - `shift-form.js` に `wishes[` を使った `name` の生成があり、`earlyWish`・`lateWish` の文字列が存在しないことをテストで検証している
    - `GET /` の HTML に、上限 12 を示す属性（`data-max-rows="12"` など）が存在することをテストで検証している
    - `shift-form.js` に「行を追加」ボタンの無効化処理（`disabled`）が含まれることをテストで検証している
    - `node --check src/main/resources/static/js/shift-form.js` が成功する（Node が使えない場合は、その旨を実行ログに記録する）
    - `./mvnw test` が成功する

- [ ] **T9. 旧仕様の用語・設定の取り残しを除去し、全テストと静的解析を確認する（全仕様）**
  - 依頼事項：`git grep -n "早番\|遅番\|earlyWish\|lateWish\|breakTimes\|20名\|MAX_EMPLOYEE_COUNT = 20" -- src README.md` を実行し、旧仕様の記述が残っていれば新仕様に合わせて直す（`docs/` は更新済みのため対象外。`README.md` に旧仕様の説明があれば新仕様に合わせて更新する）。テストコードの `@DisplayName` に旧仕様の用語が残っている場合も直す。最後に `./mvnw spotless:apply` を実行し、`./mvnw test` で全テストと静的解析（Spotless・Checkstyle）を確認する
  - 対象ファイル：`src/`、`README.md`
  - 完了条件：
    - 上記の `git grep` が 0 件（新仕様の説明として意図的に「早番・遅番は廃止」と書いた箇所は除く。その場合は理由を実行ログに記録する）
    - `./mvnw test` で全テストが成功する（テスト件数を実行ログに記録する）
    - Checkstyle の違反が 0 件である（`target/checkstyle-result.xml` に `<error` が存在しない）

## 実行ログ

<!-- implementer が試行結果（失敗理由・リトライ回数）を追記する欄。作成時は空のままにする -->
