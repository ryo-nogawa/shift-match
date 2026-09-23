# Todo: ShiftAssignmentService に入力チェック（V-1・V-2・V-4）を実装する

- Issue: #6
- ブランチ: feature/6-input-validation
- 版: v1
- 対象仕様: V-1, V-2, V-4
- 作成日: 2026-09-23

## 前提

- 読むべきドキュメント・規約：
  - `docs/specifications.md`（要件定義。特に 4.2 節 入力チェック、6 章 不成立時の仕様）
  - `docs/requirements.md`
  - `.claude/rules/tdd.md`（Red → Green → Refactor を厳守）
  - `.agents/rules/test.md`（`@Nested`＋`@DisplayName`（Given-When-Then）、仕様 ID を `[V-1]` 形式で先頭に付与、AssertJ 禁止・標準 Assertions のみ）
  - `.agents/rules/naming.md`（クラス UpperCamelCase、実装クラスはインタフェース名 + `Impl`）
  - `.agents/rules/javadoc.md`（public/protected/package-private に Javadoc 必須、private とテストコードは対象外）
  - `.agents/rules/lambda.md`（関数型インタフェースはラムダ式で実装し、メソッド参照 `Class::method` は禁止）
  - `.agents/rules/formatting.md`（`./mvnw spotless:apply` で整形してからコミット）
- 設計方針（既存コードとの整合性を優先し、破壊的変更を避ける）：
  - `ShiftAssignmentService.assign(List<Employee>)` の戻り値の型（`Optional<AssignmentResult>`）と、既存の成功／不成立の意味は**変更しない**
  - **V-1**：`assign` の先頭で、`employees` から氏名が空（`null` または `String#isBlank()`）の要素を除外してから、既存の総当たりロジックを実行する。除外はエラーではないので、例外を投げたり `Optional.empty()` を返したりしない。既存テスト（全員が有効な氏名を持つケース）の挙動は変えないこと
  - **V-2**：新しいドメイン record `DuplicateNameError`（`String name`、`List<Integer> rowIndexes`）を作成する。`rowIndexes` は元の `employees` リストにおけるインデックス（氏名が空の行は対象外）。`ShiftAssignmentService` に新メソッド `List<DuplicateNameError> findDuplicateNames(List<Employee> employees)` を追加する。氏名が空の行はまず除外してから重複を判定し、重複がなければ空リストを返す。同じ氏名が 3 件以上ある場合も 1 つの `DuplicateNameError` にまとめ、該当する全ての行インデックスを含める。このメソッドは `assign` とは独立しており、`assign` 自体は重複チェックを行わない（呼び出し順序の制御は将来の Controller の責務）
  - **V-4**：新しいプロダクションコードは不要。V-1 のフィルタリングにより、有効な従業員が 4 名未満なら組み合わせが 1 件も生成されず、既存の「総当たり結果が 0 件なら `Optional.empty()` を返す」ロジックにより自然に不成立となる。これを保証する検証テストのみ追加する
  - V-3（希望が ◎／○／× 以外）、Controller・画面・JavaScript は本 Todo の対象外
- 各 Todo の実装後、対象クラスのテストだけでなく `./mvnw test` 全体も実行し、既存テストを壊していないか確認すること

## Todo

- [x] **T1. V-1（氏名未入力の行を除外する）を実装する**
  - 依頼事項：
    - `ShiftAssignmentServiceImplTest` に、氏名が空文字列の行・空白のみの行を含む 5 名（うち有効な氏名は 4 名）を入力したとき、その行が算出対象から除外され、残り 4 名で通常どおり割り当てが行われることを検証するテストを先に書き、RED（現状は空行も候補に含まれてしまい期待と異なる結果になる、または `NullPointerException` 等になる）を確認する
    - `ShiftAssignmentServiceImpl#assign` の先頭で、`employees` から `name` が `null` または `isBlank()` である要素を除外した新しいリストを作り、以降の処理はそのリストに対して行うよう実装する
    - `@DisplayName` の先頭に `[V-1]` を付ける
  - 対象ファイル：
    - `src/main/java/com/example/shiftmatch/service/ShiftAssignmentServiceImpl.java`
    - `src/test/java/com/example/shiftmatch/service/ShiftAssignmentServiceImplTest.java`
  - 完了条件：
    - `./mvnw test -Dtest=ShiftAssignmentServiceImplTest` が成功する
    - `@DisplayName` の先頭に `[V-1]` が付いたテストが存在する
    - テストは、氏名が空／空白のみの行が処理対象から除外されることを検証している
    - 既存のテスト（氏名がすべて有効なケース）が引き続き成功する

- [x] **T2. `DuplicateNameError` レコードと `findDuplicateNames` の骨格（重複なしケース）を実装する**
  - 依頼事項：
    - `DuplicateNameError` record を作成する。フィールドは `String name`、`List<Integer> rowIndexes` とする
    - `ShiftAssignmentService` インタフェースに `List<DuplicateNameError> findDuplicateNames(List<Employee> employees)` を宣言する（Javadoc 必須）
    - `ShiftAssignmentServiceImpl` に実装を追加する。まず「有効な氏名がすべて異なる（重複なし）とき、空リストが返る」というテストを先に書き、RED（メソッドが存在しないためコンパイルエラー、実装後は空リストを返さないなら失敗）を確認してから実装する
    - このテストクラスは新規ファイル `ShiftAssignmentServiceImplFindDuplicateNamesTest.java` として作成してよい（既存の `ShiftAssignmentServiceImplTest` に追記してもよいが、後続 Todo との兼ね合いで新規ファイルを推奨）
    - `@DisplayName` の先頭に `[V-2]` を付ける
  - 対象ファイル：
    - `src/main/java/com/example/shiftmatch/domain/DuplicateNameError.java`
    - `src/main/java/com/example/shiftmatch/service/ShiftAssignmentService.java`
    - `src/main/java/com/example/shiftmatch/service/ShiftAssignmentServiceImpl.java`
    - `src/test/java/com/example/shiftmatch/service/ShiftAssignmentServiceImplFindDuplicateNamesTest.java`
  - 完了条件：
    - `./mvnw test -Dtest=ShiftAssignmentServiceImplFindDuplicateNamesTest` が成功する
    - `@DisplayName` の先頭に `[V-2]` が付いている
    - 重複がないケースで空リストが返ることを検証するテストが存在する
    - `DuplicateNameError` に Javadoc が記載されている（`.agents/rules/javadoc.md`）

- [x] **T3. `findDuplicateNames` の重複ありケースを実装する**
  - 依頼事項：
    - 次の 3 パターンを検証するテストを先に書き、RED を確認してから実装する
      1. 有効な氏名のうち 1 組（2 件）が重複しているケース → `DuplicateNameError` が 1 件返り、`name` が重複した氏名、`rowIndexes` が該当する 2 つの元のインデックスと一致する
      2. 同じ氏名が 3 件以上重複しているケース → 1 件の `DuplicateNameError` にまとまり、`rowIndexes` に該当する全インデックスが含まれる
      3. 氏名が空の行を含みつつ、有効な行同士で氏名が重複しているケース → 空行はカウントされず、有効な行のインデックスのみが `rowIndexes` に含まれる（元のリストでのインデックスを使うこと。V-1 除外後の詰め直したインデックスではない）
    - 重複判定・グループ化のロジックを実装する
    - `@DisplayName` の先頭に `[V-2]` を付ける
  - 対象ファイル：
    - `src/main/java/com/example/shiftmatch/service/ShiftAssignmentServiceImpl.java`
    - `src/test/java/com/example/shiftmatch/service/ShiftAssignmentServiceImplFindDuplicateNamesTest.java`
  - 完了条件：
    - `./mvnw test -Dtest=ShiftAssignmentServiceImplFindDuplicateNamesTest` が成功する
    - `@DisplayName` の先頭に `[V-2]` が付いている
    - 上記 3 パターンがそれぞれテストされ、`rowIndexes` が元のリストのインデックスと一致することを検証している

- [x] **T4. V-4（有効な従業員が4名未満は不成立）を検証するテストを追加する**
  - 依頼事項：
    - 氏名が空の行を含むことで見かけ上は 5 行あるが、有効な氏名を持つ従業員が 3 名以下になるケースで、`assign` が `Optional.empty()` を返すことを検証するテストを先に書く（新しいプロダクションコードは不要な想定だが、先に RED を確認してから GREEN であることを確かめること）
    - `@DisplayName` の先頭に `[V-4]` を付ける
  - 対象ファイル：
    - `src/test/java/com/example/shiftmatch/service/ShiftAssignmentServiceImplTest.java`
  - 完了条件：
    - `./mvnw test -Dtest=ShiftAssignmentServiceImplTest` が成功する
    - `@DisplayName` の先頭に `[V-4]` が付いている
    - テストは、氏名が空の行を除いた有効な従業員が 4 名未満のとき `assign` が空の `Optional` を返すことを検証している

- [x] **T5. 全体テストの成功を確認する**
  - 依頼事項：
    - `./mvnw test` を実行し、Spotless・Checkstyle を含めて全テストが成功することを確認する
    - 未整形箇所があれば `./mvnw spotless:apply` を実行してから再確認する
    - Javadoc 未記載の public/protected/package-private なクラス・メソッドがないか確認する（`.agents/rules/javadoc.md`）
  - 対象ファイル：なし（確認のみ）
  - 完了条件：
    - `./mvnw test` が成功する（テスト失敗・Spotless 違反・Checkstyle 違反のいずれもない）

## 実行ログ

<!-- implementer が試行結果（失敗理由・リトライ回数）を追記する欄。作成時は空のままにする -->
