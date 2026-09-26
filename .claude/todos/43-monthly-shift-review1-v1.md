# Todo: 月間シフト算出 Codex レビュー 1 ラウンド目の指摘対応

- Issue: #43（PR: #48）
- ブランチ: feature/43-monthly-shift
- 版: v1
- 対象仕様: V-1、V-2、V-3、V-4、V-6、V-7、V-8、V-9、F-5
- 作成日: 2026-09-26

## 前提

- 読むべきドキュメント・規約：`target/reviews/code-quality-review.md`（指摘の詳細）、`docs/specifications.md` の 4.1 節・4.2 節・6 章、`.claude/rules/tdd.md`、`.agents/rules/test.md`・`javadoc.md`・`exception.md`・`lambda.md`・`formatting.md`・`checkstyle.md`
- 変更してよい範囲は前回と同じ（`domain/`・`service/` とそのテスト）。`pom.xml`・`controller/`・`templates/`・`static/`・`persistence/` は変更しない。新しい依存ライブラリは追加しない
- 振る舞いの変更を伴う R1〜R3 は、必ずテストを先に書き、アサーションで失敗（RED）することを確認してから実装する
- 対象は `service/MonthlyInputValidator.java`・`service/MonthlyShiftServiceImpl.java`・`domain/DailyShiftResult.java` と、`src/test/java/com/example/shiftmatch/{service,domain}/` の月間関連テスト（`MonthlyInputValidatorTest`・`MonthlyShiftServiceImplTest`・`SelectionRationaleLoggerTest`・`WishResolverTest`、`domain/` の月間関連の各テスト）
- 整形：コミット前に `./mvnw spotless:apply`。コミットは Todo 1 件ごとに Conventional Commits（日本語）で、末尾に `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>` を付ける

## Todo

- [x] **R1. 個別変更の時間帯も V-3 で検証する（MUST・V-3）**
  - 依頼事項：`MonthlyInputValidator.validateTimeRanges` で、有効な従業員の氏名に一致する `input.adjustments()` も走査する。「休み」の個別変更は時刻を無視し、それ以外は基本シフトと同じ規則（開始・終了が未選択＝null、7:30〜18:30 の 30 分単位以外、開始 ≧ 終了）で検証し、氏名と日付を `message` に含む `code = "V-3"` のエラーを返す。氏名が有効な従業員と一致しない個別変更は無視する（エラーにしない）。エラーの並びは、基本シフトのエラー（行順・曜日順）の後に個別変更のエラー（リストの順）とする
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/MonthlyInputValidator.java`、`src/test/java/com/example/shiftmatch/service/MonthlyInputValidatorTest.java`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `[V-3]` 付きで、個別変更の開始・終了が null／30 分単位でない値／範囲外／開始 ≧ 終了のそれぞれがエラーになり、`message` に氏名と日付が含まれるテストがある
    - 休みの個別変更は時刻が null でもエラーにならない、氏名が一致しない個別変更は不正な値でもエラーにならないテストがある
    - `./mvnw test -Dtest=MonthlyInputValidatorTest` が成功する

- [x] **R2. 欠損入力（名前が null、対象月が null）をエラーとして扱う（MUST・V-1、V-8）**
  - 依頼事項：従業員名が null の行は、`isBlank()` の前に null を判定して V-1 の除外対象にする（`NullPointerException` にしない）。`MonthlyInputValidator` と `MonthlyShiftServiceImpl`（`WishResolver` を含む、氏名を扱う箇所すべて）で null 安全にする。`input.month()` が null の場合は `HolidayService` を呼ばずに V-8 のエラーとし、V-9 の検証は行わない（対象月がないため）。その場合 `MonthlyShiftServiceImpl.create` は `InvalidMonthlyInputException` を投げる
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/MonthlyInputValidator.java`、`MonthlyShiftServiceImpl.java`、`WishResolver.java`（必要な場合）、対応するテスト
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `[V-1]` 従業員名が null の行が除外され例外にならない（`validate` と `create` の両方）、`[V-8]` 対象月が null のとき V-8 のエラーになり `HolidayService` が呼ばれないテストがある
    - `./mvnw test -Dtest=MonthlyInputValidatorTest+MonthlyShiftServiceImplTest` が成功する

- [x] **R3. エラーの行番号を、空行を除く前の入力行で示す（MUST・V-2、V-3、V-6、V-7）**
  - 依頼事項：V-1 で空行を除外する前の、元のリストでの位置（0 始まり）を従業員と対にして保持し、表示するときだけ 1 を足す（`message` は元の入力行の 1 始まりの行番号）。V-2 のメッセージには重複した全ての行番号を含める。V-3（基本シフト）・V-6・V-7 も元の行番号を示す。先頭や途中に空行がある入力でも行番号がずれないようにする
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/MonthlyInputValidator.java`、`src/test/java/com/example/shiftmatch/service/MonthlyInputValidatorTest.java`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - 1 行目が空で 2 行目が不正なとき、V-3・V-6・V-7 のメッセージが「2 行目」を示すテストがある（3 件それぞれ）
    - V-2 のメッセージに、重複した全ての行の番号（例：空行を挟んで 2 行目と 4 行目）が含まれるテストがある
    - `./mvnw test -Dtest=MonthlyInputValidatorTest` が成功する

- [x] **R4. `MonthlyInputValidator` を注入し、V-4 と検証エラーのテストを強化する（SHOULD）**
  - 依頼事項：`MonthlyShiftServiceImpl` のコンストラクタで `MonthlyInputValidator` を引数として受け取り（`new` しない）、`MonthlyShiftServiceImplTest` などのコンストラクタ呼び出しを直す。`WishResolver` は状態を持たない値オブジェクト相当なので現状のままとし、クラスの Javadoc にその旨を 1 行で書く。テストの強化：`[V-4]` のテストは、Mockito の未スタブで `assignment` が null にならないよう `ShiftAssignmentService` を適切にスタブし（成立する日は結果あり、7 名の日は `Optional.empty()`）、`Optional.empty()` を厳密に検証し、他の日の算出が続くことも検証する。検証エラーのテストは、`assign` を一度も呼ばないことを `verifyNoInteractions(assignmentService)`（または `never().assign(anyList())`）で検証する
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/MonthlyShiftServiceImpl.java`、`src/test/java/com/example/shiftmatch/service/MonthlyShiftServiceImplTest.java`
  - 完了条件：
    - `MonthlyShiftServiceImpl` の中に `new MonthlyInputValidator` が存在しない
    - V-4 のテストが `Optional.empty()` と `availableCount`（7）を検証し、検証エラーのテストが `assign` の全呼び出しを禁止している
    - `./mvnw test -Dtest=MonthlyShiftServiceImplTest` が成功する

- [ ] **R5. Javadoc のコード表記を `{@code}` に直す（MUST）**
  - 依頼事項：`domain/DailyShiftResult.java` の Javadoc にある Markdown のバッククォート表記を `{@code ...}` に直す。ほかの本 Issue で追加した `domain/`・`service/` の Javadoc にも同じ違反がないか `grep -n '`' src/main/java/com/example/shiftmatch/{domain,service}/*.java` で確認し、あれば直す（本 Issue で追加したファイルのみ。既存ファイルは直さない）
  - 対象ファイル：`src/main/java/com/example/shiftmatch/domain/DailyShiftResult.java`（ほか、本 Issue で追加したファイル）
  - 完了条件：
    - 本 Issue で追加した Java ファイルの Javadoc にバッククォートが残っていない
    - `./mvnw test` が成功する

- [ ] **R6. 追加したテストを test.md の規約に合わせる（MUST）**
  - 依頼事項：`.agents/rules/test.md` を読み、本 Issue で追加・変更したテストクラス（`git diff main --name-only -- src/test` で列挙）を、正常系・異常系などの `@Nested` クラスに整理し、全テストメソッドの `@DisplayName` を「仕様 ID ＋ `Given: …、When: …、Then: …`」の日本語形式に直す（仕様 ID がある場合は先頭に `[V-3]` などを付ける）。テストの中身（期待値）は変えない。既存の本 Issue 外のテストは変更しない
  - 対象ファイル：`git diff main --name-only -- src/test` に出る本 Issue で追加したテストファイルすべて
  - 完了条件：
    - 追加した各テストクラスに `@Nested` が存在し、全テストメソッドの `@DisplayName` が `Given:`・`When:`・`Then:` を含む
    - `git diff` 上、テストの期待値（アサーション）が変更されていない（`@DisplayName`・クラス構造・`@Nested` 化のための移動のみ）
    - `./mvnw test` が成功する

- [ ] **R7. 全テストと静的解析を通す**
  - 依頼事項：`./mvnw spotless:apply` の後、`./mvnw test` を実行して、全テスト・Spotless・Checkstyle が成功することを確認する。`git diff main --stat` に `pom.xml`・`controller/`・`templates/`・`static/`・`persistence/` が含まれないことも確認する
  - 対象ファイル：本 Issue で変更した全ファイル
  - 完了条件：
    - `./mvnw test` で全テストが成功する（Spotless・Checkstyle の違反 0 件）
    - `git diff main --stat` に `pom.xml`・`controller/`・`templates/`・`static/`・`persistence/` が含まれない

## 実行ログ

<!-- implementer が試行結果（失敗理由・リトライ回数）を追記する欄。作成時は空のままにする -->
