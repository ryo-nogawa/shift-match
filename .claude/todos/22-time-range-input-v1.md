# Todo: 希望入力を勤務可能時間帯（開始・終了）＋休み方式へ変更

- Issue: #22
- ブランチ: feature/22-time-range-input
- 版: v1
- 対象仕様: F-1, F-4, V-3, V-4, V-5, H-3, 5.2, 5.3, 5.4, 7 章, 8 章
- 作成日: 2026-09-25

## 前提

- 読むべきドキュメント・規約：`docs/specifications.md`（3〜8 章）、`docs/requirements.md` 8 章、`AGENTS.md`、`.claude/rules/tdd.md`、`.agents/rules/test.md`、`.agents/rules/naming.md`、`.agents/rules/javadoc.md`
- 新仕様の要点
  - 従業員の入力は「氏名・休み（チェック）・開始・終了」です。開始・終了は 7:30〜18:30 の 30 分単位（`"07:30"`, `"08:00"`, …, `"18:30"` の 23 通り。値は `HH:mm` の 24 時間表記・前 0 埋め）
  - H-3：枠の勤務時間が入力時間帯に**完全に含まれる**（枠の開始 ≧ 入力の開始 かつ 枠の終了 ≦ 入力の終了）ときだけ割り当て可。「休み」の人は割り当てない
  - スコア（5.2）：割り当てた 8 名の「入力時間帯の長さ − 枠の勤務時間」（分）の合計。**最小**の案を採用（0 が最良）。同点では更新しない（`<` で更新し、`<=` にしない）。5.3 の列挙順（枠 1→6、入力順インデックスの辞書順）と 5.4 の DP の考え方は現行のまま「最大」を「最小」に読み替える
  - V-4：有効な従業員（氏名あり）のうち「休み」でない人が 8 名未満なら不成立（エラーではない）。V-5：有効な従業員（休みを含む）が 13 名以上ならエラー
  - 未出勤者（7 章）は「割り当てられなかった従業員」と「休み」の従業員の両方（入力順）
- 枠ごとの勤務時間の分数：枠 1=420、枠 2=450、枠 3=480、枠 4=450、枠 5=540、枠 6=570
- ビルドを常に通すための移行方針（重要）
  - `Employee`（`src/main/java/com/example/shiftmatch/domain/Employee.java`）は現在 `(String name, List<Wish> wishes)`。T2 で新しい要素を**追加**し、旧 2 引数コンストラクタ `Employee(String name, List<Wish> wishes)` を**移行用として残します**。これにより既存テストがコンパイルできます。T14 で旧要素を削除します
  - 旧仕様（◎／○／×・スコア最大）を検証していた既存テストは、仕様変更により期待値が無効になります。それらは、対応する新テストへ**書き換えるか、対応する Todo の中で削除**して構いません（仕様外の値への書き換えや `@Disabled` は禁止）。削除・書き換えしたテスト名は `## 実行ログ` に記録します
  - 各 Todo の終わりで `./mvnw test` がコンパイルエラーなく実行できる状態にします（Spotless 違反は `./mvnw spotless:apply` で直します）
- テストは Given-When-Then の `@DisplayName`（先頭に `[H-3]` 形式の仕様 ID）、`@Nested` でグルーピング、AssertJ 等は使わず `org.junit.jupiter.api.Assertions` のみ（`.agents/rules/test.md`）
- 新しい依存ライブラリは追加しない。DB・セッションを使わない

## Todo

- [x] **T1. `ShiftSlot` に勤務時間の分数を返す `workMinutes()` を追加する**
  - 依頼事項：`ShiftSlot` に `public int workMinutes()`（`startTime` から `endTime` までの分数）を追加する。Javadoc を書く（`.agents/rules/javadoc.md`）。関係仕様：5.2
  - 対象ファイル：`src/main/java/com/example/shiftmatch/domain/ShiftSlot.java`、`src/test/java/com/example/shiftmatch/domain/ShiftSlotTest.java`
  - 完了条件：
    - テストを先に書き、`workMinutes()` 未実装（または戻り値が違う）でアサーション失敗（RED）することを確認した
    - 枠 1〜6 の `workMinutes()` が 420, 450, 480, 450, 540, 570 であることを検証するテストが存在する（`@DisplayName` 先頭に `[5.2]`）
    - `./mvnw test -Dtest=ShiftSlotTest` が成功する

- [x] **T2. `Employee` に「休み・開始・終了」を追加し、ファクトリメソッドを用意する**
  - 依頼事項：`Employee` を `record Employee(String name, List<Wish> wishes, boolean off, LocalTime start, LocalTime end)` に拡張する。旧 2 引数コンストラクタ `Employee(String name, List<Wish> wishes)`（`off=false, start=null, end=null`）は移行用として残す（Javadoc に「T14 で削除予定の移行用」と明記）。`wishes` の件数検証（6 件）と不変リスト化は現状どおり残す。静的ファクトリ `Employee.working(String name, LocalTime start, LocalTime end)`（`off=false`）と `Employee.onLeave(String name)`（`off=true, start=null, end=null`）を追加する。この 2 つは `wishes` に `Wish.UNAVAILABLE` を 6 件入れる（移行期間の暫定値）。関係仕様：F-1、4.1
  - 対象ファイル：`src/main/java/com/example/shiftmatch/domain/Employee.java`、`src/test/java/com/example/shiftmatch/domain/EmployeeTest.java`
  - 完了条件：
    - テストを先に書き、ファクトリ未実装でコンパイル・アサーションが失敗することを確認した（空のメソッドを用意してアサーション失敗まで進めた）
    - `working` で作った従業員の `name()`・`off()`（false）・`start()`・`end()` が取得できるテストが存在する（`[F-1]`）
    - `onLeave` で作った従業員の `off()` が true、`start()`・`end()` が null であるテストが存在する（`[F-1]`）
    - 既存の `EmployeeTest` のテストがすべて成功する
    - `./mvnw test` がコンパイルエラーなく実行でき、失敗が 0 件である

- [x] **T3. `Employee.canWork(ShiftSlot)` で H-3（枠が入力時間帯に完全に含まれる）を判定する**
  - 依頼事項：`public boolean canWork(ShiftSlot slot)` を追加する。`off` が true、または `start`・`end` のいずれかが null なら false。そうでなければ `!slot.startTime().isBefore(start) && !slot.endTime().isAfter(end)` のとき true。関係仕様：H-3
  - 対象ファイル：`src/main/java/com/example/shiftmatch/domain/Employee.java`、`src/test/java/com/example/shiftmatch/domain/EmployeeTest.java`
  - 完了条件：
    - テストを先に書き RED を確認した
    - 次のテストが存在し `@DisplayName` 先頭に `[H-3]` がある：8:00〜17:00 の人は枠 2（8:00〜15:30）・枠 3・枠 4 に入れる／枠 5（〜18:00）には入れない／枠 1（7:30 開始）には入れない
    - 境界のテストが存在する：入力の開始＝枠の開始、入力の終了＝枠の終了のとき true
    - 休みの従業員は全枠で false のテストが存在する
    - 開始または終了が null（旧コンストラクタ生成を含む）のとき false のテストが存在する
    - `./mvnw test -Dtest=EmployeeTest` が成功する

- [x] **T4. `Employee.gapMinutes(ShiftSlot)` で「ずれ」（分）を計算する**
  - 依頼事項：`public int gapMinutes(ShiftSlot slot)` を追加する。戻り値は「入力時間帯の長さ（分）− `slot.workMinutes()`」。`canWork(slot)` が false のときは `IllegalStateException` を投げる（Javadoc の `@throws` に書く）。関係仕様：5.2
  - 対象ファイル：`src/main/java/com/example/shiftmatch/domain/Employee.java`、`src/test/java/com/example/shiftmatch/domain/EmployeeTest.java`
  - 完了条件：
    - テストを先に書き RED を確認した
    - 8:00〜17:00（540 分）の人が枠 2（450 分）に入ると 90 のテストが存在する（`[5.2]`）
    - 入力時間帯と枠がちょうど一致（例：9:00〜18:30 と枠 6）すると 0 のテストが存在する
    - 入れない枠を指定すると `IllegalStateException` になるテストが存在する
    - `./mvnw test -Dtest=EmployeeTest` が成功する

- [ ] **T5. サービスの割り当てを H-3 と「休み」に対応させる（V-4 を含む）**
  - 依頼事項：`ShiftAssignmentServiceImpl.assign` を次のとおり変更する。（1）候補は「氏名が空でなく、かつ `off` が false」の従業員のみ。候補が 8 名（`ShiftSlot.totalEmployees()`）未満なら `Optional.empty()`（V-4）。（2）組の列挙（`combinationHelper` 内の `wishes[i][slotIndex] != Wish.UNAVAILABLE` 判定）を `employee.canWork(slot)` に置き換える。希望配列 `Wish[][]` は使わず、候補の `List<Employee>` を渡す形に整理してよい。（3）未出勤者（`unassignedEmployees`）は、割り当てられなかった有効な従業員と「休み」の従業員の**両方を入力順**で返す。スコアの計算方式（◎ の数・最大化）はこの Todo では変更せず、T6 で変更する（この Todo では `buildResult` の score は当面 0 を返してよい）。旧テスト `ShiftAssignmentServiceImplTest` は、旧仕様（◎／○／×）を前提としたものを新仕様の `Employee.working(...)`／`Employee.onLeave(...)` を使うテストへ書き換える。関係仕様：H-3、V-4、7 章（未出勤者）
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/ShiftAssignmentServiceImpl.java`、`src/test/java/com/example/shiftmatch/service/ShiftAssignmentServiceImplTest.java`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - 次のテストが存在し、`@DisplayName` の先頭に仕様 ID がある：
      - `[H-3]` 全員が 7:30〜18:30 の 8 名 → 割り当てが成立し、8 名分の割り当てが枠 1（2 名）・2・3・4・5・6（2 名）の順で返る
      - `[H-3]` 8 名のうち 1 名が 9:00〜18:30 だけを入力（枠 1〜3 に入れない）→ 枠 6 など入れる枠にだけ割り当てられる、または入れる枠がなく不成立になる（入力例で期待値を決めて検証する）
      - `[H-3]` 誰も入れない枠が 1 つでもある（例：全員 9:00〜16:30 で枠 1 と枠 6 に入れる人がいない）→ `Optional.empty()`
      - `[V-4]` 8 名のうち 1 名が休み（休みでない人が 7 名）→ `Optional.empty()`
      - `[V-4]` 9 名のうち 1 名が休み（休みでない人が 8 名）→ 成立し、休みの人は割り当てに含まれない
      - `[F-4]` 未出勤者に、余った従業員と休みの従業員の両方が入力順で入る
    - 氏名が空の行は従来どおり処理対象から除外される（既存の該当テストが成功、または `Employee.working(" ", ...)` で書き換え済み）
    - 旧仕様（◎ の数）を検証していたテストのうち、新仕様で期待値が無効になったものを削除・書き換えした場合、`## 実行ログ` にテスト名を記録した
    - `./mvnw test -Dtest=ShiftAssignmentServiceImplTest` が成功する
    - `./mvnw test` がコンパイルエラーなく実行でき、失敗が 0 件である（ControllerTest 等が旧コンストラクタで従来どおり成功している）

- [ ] **T6. スコアを「ずれの合計（分）」の最小化に変更する（DP と復元）**
  - 依頼事項：DP を最小化に変更する。`computeMaxScore` → `computeMinScore` などへ改名し、各組のスコアを `employee.gapMinutes(slot)` の合計にする。更新条件は `maxScore == IMPOSSIBLE || totalScore < minScore`（同点で更新しない）。復元（`reconstructAssignment`）は「その組のスコア＋次の状態の最小スコア＝目標スコア」を満たす最初の組を選ぶ現行ロジックのまま。`buildResult` の `score` は選ばれた 8 名の `gapMinutes` の合計にする。`AssignmentResult.score` の意味が変わるため、`AssignmentResult` の Javadoc（「スコア」）を「ずれの合計（分）」に直す。関係仕様：5.2、5.4
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/ShiftAssignmentServiceImpl.java`、`src/main/java/com/example/shiftmatch/domain/AssignmentResult.java`、`src/test/java/com/example/shiftmatch/service/ShiftAssignmentServiceImplTest.java`
  - 完了条件：
    - テストを先に書き RED を確認した
    - `[5.2]` 全員が 7:30〜18:30（660 分）の 8 名 → `score` が 1380（＝ 8×660 − 3900。3900 は枠 1×2＋枠 2＋枠 3＋枠 4＋枠 5＋枠 6×2 の勤務分数の合計）のテストが存在する
    - `[5.2]` 7:30〜18:30 の 8 名に加えて 7:30〜14:30（420 分）の 1 名（入力順で最後）→ その 1 名が枠 1 に割り当てられ、`score` が 1140（＝ 7×660 ＋ 420 − 3900）のテストが存在する
    - `[5.2]` ずれの合計が小さい案が採用される、上記以外の 1 ケース（入力例と期待値を Todo の実装者が自分で計算し、`@DisplayName` に書く）が存在する
    - `./mvnw test -Dtest=ShiftAssignmentServiceImplTest` が成功する
    - `./mvnw test` がコンパイルエラーなく実行でき、失敗が 0 件である

- [ ] **T7. 同点時は入力順で最初の案を採用することを検証する（5.3）**
  - 依頼事項：ずれの合計が同じ案が複数ある入力で、5.3 節の列挙順（枠 1 → 6、入力順インデックスの辞書順）で最初の案が返ることをテストで固定する。実装が既に満たす場合はテストだけの追加でよい（その場合、Red を確認できないため、追加したテストが **実装を意図的に `<=` に変えると失敗する** ことを一度確認してから元に戻す）。関係仕様：5.3
  - 対象ファイル：`src/test/java/com/example/shiftmatch/service/ShiftAssignmentServiceImplTest.java`（必要なら `ShiftAssignmentServiceImpl.java`）
  - 完了条件：
    - `[5.3]` 全員が 7:30〜18:30 の 9 名（同点）→ 先頭から 8 名（入力順の 1〜8 人目）が割り当てられ、9 人目が未出勤者になり、枠 1 に 1・2 人目、枠 2 に 3 人目、枠 3 に 4 人目、枠 4 に 5 人目、枠 5 に 6 人目、枠 6 に 7・8 人目が入るテストが存在する
    - 更新条件を `<=` に変えるとこのテストが失敗することを確認し、元に戻した（実行ログに記録）
    - `./mvnw test -Dtest=ShiftAssignmentServiceImplTest` が成功する

- [ ] **T8. フォームを「休み・開始・終了」に変更し、V-3 の入力チェックを実装する**
  - 依頼事項：
    1. `EmployeeForm` の `wishes`（List<String>）を削除し、`boolean off`、`String start`、`String end` を追加する（Lombok `@Getter`/`@Setter`）
    2. `ShiftController.convertToEmployees` を、`EmployeeForm` から `Employee`（正規コンストラクタ。`wishes` には `Wish.UNAVAILABLE` を 6 件入れる暫定値）へ変換する形に変更する。`start`/`end` は `HH:mm` として解析し、空・不正な文字列は `null` にする。`off` が true の行は `start`/`end` を無視して `null` にする
    3. V-3：氏名が入力された行のうち、`off` でない行について次をチェックし、エラーを `InvalidTimeRangeError`（新規 `record InvalidTimeRangeError(int rowIndex, String message)`、`domain` パッケージ）に集める。メッセージは「開始が未選択です」「終了が未選択です」「開始は選択肢にありません」「終了は選択肢にありません」「開始は終了より前にしてください」。選択肢は 07:30 から 18:30 まで 30 分刻みの 23 通り。エラーが 1 件でもあれば従来どおり算出せずに `index` を返し、モデルに `timeRangeErrors`（旧 `wishErrors`）を入れる
    4. `InvalidWishError` はまだ削除しない（T14 で削除）。コントローラーからの参照だけをなくす
    5. `ShiftControllerTest` の入力フォーム送信は、`employees[i].wishes[j]` の代わりに `employees[i].off`（チェック時 `true`）、`employees[i].start`、`employees[i].end` を使うよう書き換える。旧 `WishValidation` のグループは V-3 の新テストに置き換える
    - 関係仕様：V-3、F-1、8 章
  - 対象ファイル：`src/main/java/com/example/shiftmatch/controller/EmployeeForm.java`、`src/main/java/com/example/shiftmatch/controller/ShiftController.java`、`src/main/java/com/example/shiftmatch/domain/InvalidTimeRangeError.java`（新規）、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - テストを先に書き RED を確認した
    - `[V-3]` の次のテストが存在する：開始が未選択 → エラーで算出されない（`shiftAssignmentService.assign` が呼ばれない。`verify(..., never())`）、終了が未選択、開始が選択肢外（例：`08:10`）、開始＝終了、開始＞終了、いずれも該当行番号（1 始まり）を含むエラーが画面に出る
    - `[V-3]` 休みにチェックした行は開始・終了が空でもエラーにならず、その行は `assign` に渡す従業員が休み（`off()` が true）である
    - `[V-1]` 氏名が空の行は、開始・終了が不正でもエラーにならず処理対象から除外される
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
    - `./mvnw test` がコンパイルエラーなく実行でき、失敗が 0 件である

- [ ] **T9. 時刻の選択肢（30 分単位）をモデルに設定する**
  - 依頼事項：`ShiftController` の `@ModelAttribute("slotLabels")` を、`@ModelAttribute("timeOptions")` に置き換える。値は `List<String>` で、`"07:30"`, `"08:00"`, …, `"18:30"` の 23 件（24 時間表記・前 0 埋め）。V-3 の選択肢判定（T8）でもこのリストを使う（重複する定数を持たない）。`ShiftControllerTest` の `PostReturnPathsIncludeSlotLabels` グループは `timeOptions` を検証する内容へ書き換える（GET・POST 成功・POST エラーの全経路でモデルに含まれること）。関係仕様：8 章
  - 対象ファイル：`src/main/java/com/example/shiftmatch/controller/ShiftController.java`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - テストを先に書き RED を確認した
    - GET `/`、POST `/shift`（成功・不成立・入力エラー）のすべてで、モデルの `timeOptions` が 23 件で先頭 `07:30`・末尾 `18:30` のテストが存在する（`[F-1]`）
    - `slotLabels` への参照がプロダクションコードとテストから消えている（`grep -rn slotLabels src/` が 0 件。テンプレートは T10 で直すため、この時点でテンプレートに残っていてよい）
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する

- [ ] **T10. 入力表のテンプレートを「氏名・休み・開始・終了・削除」に変更する**
  - 依頼事項：`index.html` の入力表を次のとおり変更する。（1）列は「氏名・休み・開始・終了・削除」。（2）休みは `th:field="*{employees[__${stat.index}__].off}"` のチェックボックス。（3）開始・終了は `<select>`（`th:field` で `start`/`end`）で、先頭に未選択（`value=""`、表示「-- 未選択 --」）と `${timeOptions}` の各値（値・表示ともに `HH:mm`）。（4）旧「◎ 希望／○ 可能／× 不可」の凡例（`.wish-legend`）と枠ごとの列見出し・`data-slot-labels` を削除。（5）エラー表示は `timeRangeErrors`（`n行目 メッセージ`）に変更。（6）見出しの説明文を「従業員の勤務できる時間帯を入力すると…」に直す。（7）休みにチェックされた行では、開始・終了を選択できない（`disabled`）ようにする（サーバー描画時の再表示でもチェック済みなら `disabled`）。関係仕様：F-1、8 章
  - 対象ファイル：`src/main/resources/templates/index.html`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - テストを先に書き RED を確認した
    - `[F-1]` GET `/` のレスポンスに、`employees[0].off`、`employees[0].start`、`employees[0].end` の `name` 属性が含まれ、`employees[0].wishes` が含まれないテストが存在する
    - `[F-1]` GET `/` のレスポンスの開始・終了の選択肢に `07:30` と `18:30` が含まれ、`07:45` が含まれないテストが存在する
    - `[V-3]` 入力エラー時の画面に `timeRangeErrors` の「n行目 …」が表示されるテストが存在する
    - `[F-1]` 休みにチェックして送信し直したとき、その行の開始・終了が `disabled` で再表示されるテストが存在する
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する

- [ ] **T11. 行の追加・削除の JavaScript を新しい入力列に合わせる**
  - 依頼事項：`shift-form.js` を次のとおり変更する。（1）`createWishSelect` と、`data-slot-labels` を使う枠ごとの選択肢生成を削除する。（2）行追加時は、氏名 input・休みチェックボックス（`name="employees[N].off"`、`value="true"`）・開始 select（`employees[N].start`）・終了 select（`employees[N].end`）・削除ボタンを作る。時刻の選択肢は、テーブルの `data-time-options`（`index.html` に `th:attr="data-time-options=${#strings.listJoin(timeOptions, '|')}"` を付与）から読む。（3）休みチェックの変更時に、同じ行の開始・終了の `disabled` を切り替える。（4）行削除後のインデックス振り直しは、`employees[N]` を含む全 `input`/`select` の `name`（休みの隠しフィールド `_employees[N].off` を含む）に対して行う。（5）行の上限 12・最低 1 行は現状のまま。関係仕様：F-2、F-6、8 章
  - 対象ファイル：`src/main/resources/static/js/shift-form.js`、`src/main/resources/templates/index.html`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`（`JavaScriptAddDeleteRows` グループ）
  - 完了条件：
    - テストを先に書き RED を確認した（既存の `JavaScriptAddDeleteRows` の期待値を新仕様に書き換え、旧実装で失敗することを確認）
    - JS の内容を検証するテストで、`.off`、`.start`、`.end` の name 生成があること、`wishes` および `createWishSelect` が含まれないことを検証している（`[F-2]` `[F-6]`）
    - `[F-6]` インデックスを振り直す処理が `_employees[` の隠しフィールドも対象にしていることを検証するテスト、または JS にその旨のコメントがあることを確認した
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する

- [ ] **T12. 結果画面のスコア表示を「ずれの合計（分）」に変更する**
  - 依頼事項：`index.html` の結果部を変更する。スコアの表示を `<span class="score-num"><span th:text="${assignmentResult.score()}">0</span><small> 分</small></span>`、ラベルを「ずれの合計（入力時間帯と割り当てた枠の差。0 が最良）」にする（`/ 8` を削除）。未出勤者（`未出勤者` チップ）は「休み」の従業員も含めて表示される（サービスが返すためテンプレート変更は不要だが、表示のテストで確認する）。関係仕様：7 章、F-4
  - 対象ファイル：`src/main/resources/templates/index.html`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`（`ScoreAndUnassignedDisplay` グループ）
  - 完了条件：
    - テストを先に書き RED を確認した
    - `[F-4]` スコア 90 の結果でレスポンスに「90」と「分」が含まれ、`/ 8` が含まれないテストが存在する
    - `[F-4]` 「ずれの合計」のラベルがあり、`◎` が結果画面に含まれないテストが存在する
    - `[F-4]` 未出勤者に休みの従業員の氏名が表示されるテストが存在する
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する

- [ ] **T13. V-5（13 名以上）が「休み」を含めて数えられることをテストで固定する**
  - 依頼事項：V-5 の判定はコントローラーの `validEmployees.size() > MAX_EMPLOYEE_COUNT`（休みを含む有効行数）で既に満たされる想定。13 名（うち休み 5 名など）の入力でエラーとなり `assign` が呼ばれないテスト、12 名（休みを含む）では算出に進むテストを `EmployeeLimitAndUnassignable` グループに追加・整理する。実装が既に満たす場合は、`>` を `>=` に変えると失敗することを確認してから元に戻す。関係仕様：V-5、V-4
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`（必要なら `ShiftController.java`）
  - 完了条件：
    - `[V-5]` 13 名（休み 5 名を含む）→ エラーメッセージが表示され `assign` が呼ばれない（`verify(..., never())`）テストが存在する
    - `[V-5]` 12 名（休み 4 名を含む）→ エラーにならず `assign` が呼ばれるテストが存在する
    - `[V-4]` サービスが `Optional.empty()` を返すと「条件を満たす組み合わせが見つかりませんでした」が表示されるテストが存在する
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する

- [ ] **T14. 旧仕様の要素（`Wish`・`InvalidWishError`・移行用コンストラクタ・`wishes`）を削除する**
  - 依頼事項：`Employee` を `record Employee(String name, boolean off, LocalTime start, LocalTime end)` に変更し、`wishes` 要素・件数検証・旧 2 引数コンストラクタ・`Wish` を使うコード（`Employee.working`/`onLeave` 内の暫定値を含む）を削除する。`Wish.java` と `InvalidWishError.java` を削除する。`EmployeeTest`、`AssignmentResultTest`、`ShiftAssignmentServiceImplFindDuplicateNamesTest`、`ShiftControllerTest`、`ShiftAssignmentServiceImplTest` の `Employee` 生成を `Employee.working(...)` または `Employee.onLeave(...)`（氏名だけが必要なテストは `Employee.working(name, LocalTime.of(7, 30), LocalTime.of(18, 30))`）へ置き換える。`wishes` の件数検証を確認していたテストは削除する（要素自体が仕様から無くなったため）。ここでは振る舞いを変えない（リファクタリング）。関係仕様：F-1
  - 対象ファイル：`src/main/java/com/example/shiftmatch/domain/Employee.java`、`Wish.java`（削除）、`InvalidWishError.java`（削除）、および上記のテストファイル
  - 完了条件：
    - `grep -rniE "wish|◎|○" src/` が 0 件（コメント・文字列を含む。ただし `docs/`、`AGENTS.md` は対象外）
    - 削除した `wishes` 件数検証テスト名を `## 実行ログ` に記録した
    - `./mvnw test` が成功し、失敗が 0 件である

- [ ] **T15. CSS の残骸と `AGENTS.md` のドメイン記述を新仕様へ更新する**
  - 依頼事項：（1）`src/main/resources/static/css/shift-form.css` の `.wish-legend`（および `.w.d`・`.w.a`・`.w.u` など、◎○× 専用のスタイルで他から使われていないもの）を削除する。使われているかは `grep` で確認する。開始・終了の select と休みのチェックボックスの見た目が崩れないよう、必要最小限のスタイルを足してよい。（2）`AGENTS.md` の「ドメインの要点」を新仕様に合わせて更新する：「算出ロジック」のハード制約（H-3 を「枠が入力時間帯に完全に含まれるときだけ」に）、スコア（ずれの合計・最小化、同点で更新しない条件を `<` に）、探索の説明の「最大」→「最小」、入力チェック（V-3・V-4）、「画面とフォームバインディング」の例（`employees[0].name`、`employees[0].off`、`employees[0].start`、`employees[0].end`）。仕様書 `docs/` は変更しない（既に改訂済み）。関係仕様：5.2、8 章
  - 対象ファイル：`src/main/resources/static/css/shift-form.css`、`AGENTS.md`
  - 完了条件：
    - `grep -nE "wish|◎|○" AGENTS.md src/main/resources/static/css/shift-form.css` が 0 件
    - `AGENTS.md` に「ずれの合計」と `employees[0].start` が含まれる
    - `./mvnw test` が成功する

- [ ] **T16. 全テストと静的解析を通す**
  - 依頼事項：`./mvnw spotless:apply` で整形し、`./mvnw test` を実行する。Checkstyle 違反（`target/checkstyle-result.xml`）があれば直す。テスト件数と結果を `## 実行ログ` に記録する
  - 対象ファイル：全体
  - 完了条件：
    - `./mvnw test` で全テストが成功する（失敗 0 件）
    - Spotless の整形違反が 0 件、Checkstyle の違反が 0 件である

## 実行ログ

<!-- implementer が試行結果（失敗理由・リトライ回数）を追記する欄。作成時は空のままにする -->
