# Todo: シフト割り当ての中核アルゴリズムを実装する

- Issue: #4
- ブランチ: feature/4-shift-assignment-core
- 版: v1
- 対象仕様: F-3, H-1, H-2, H-3, H-4
- 作成日: 2026-09-23

## 前提

- 読むべきドキュメント・規約：
  - `docs/requirements.md`（要求定義。特に 5 章 業務上の制約条件、8 章 用語）
  - `docs/specifications.md`（要件定義。特に 5 章 割り当てルール、7 章 出力仕様）
  - `.claude/rules/tdd.md`（Red → Green → Refactor を厳守）
  - `.agents/rules/test.md`（`@Nested`＋`@DisplayName`（Given-When-Then）、仕様 ID を `[H-1]` 形式で先頭に付与、AssertJ 禁止・標準 Assertions のみ）
  - `.agents/rules/naming.md`（クラス UpperCamelCase、実装クラスはインタフェース名 + `Impl`）
  - `.agents/rules/javadoc.md`（public/protected/package-private に Javadoc 必須、private とテストコードは対象外）
  - `.agents/rules/lambda.md`（関数型インタフェースはラムダ式で実装し、メソッド参照 `Class::method` は禁止）
  - `.agents/rules/formatting.md`（`./mvnw spotless:apply` で整形してからコミット）
- 実装上の注意：
  - このブランチの対象はドメインロジック（モデル・Service 層）のみ。Controller・Thymeleaf・JavaScript・入力チェック V-1〜V-4 は対象外（Issue #4 のスコープ外）
  - パッケージ構成は `com.example.shiftmatch.domain`（モデル）と `com.example.shiftmatch.service`（Service）を新設する
  - 用語の対応：◎ = 希望する（優先）、○ = 可能、× = 不可（割り当てない）
  - 出力仕様（`docs/specifications.md` 7 章）は「早番の氏名」「遅番の氏名」「スコア」「未出勤者」の 4 項目
  - すべてのテストで `./mvnw test -Dtest=クラス名` を実行し、まず RED（意図した理由での失敗）を確認してから実装すること
  - 各 Todo の実装後、対象クラスのテストだけでなく `./mvnw test` 全体も実行し、既存テストを壊していないか確認すること

## Todo

- [ ] **T1. ドメインモデル（Wish enum・Employee record）を作成する**
  - 依頼事項：
    - `Wish` enum を作成する。定数は `DESIRED`（◎ 希望する）、`AVAILABLE`（○ 可能）、`UNAVAILABLE`（× 不可）の 3 つとする
    - `Employee` record を作成する。フィールドは `String name`、`Wish earlyWish`、`Wish lateWish` とする
    - まずテストを書き、RED（コンパイルは通るがアサーションで失敗、または意図した失敗）を確認してから実装する。テスト内容は「有効な値で `Employee` を生成すると、`name`・`earlyWish`・`lateWish` がそれぞれ取得できる」という振る舞いでよい
  - 対象ファイル：
    - `src/main/java/com/example/shiftmatch/domain/Wish.java`
    - `src/main/java/com/example/shiftmatch/domain/Employee.java`
    - `src/test/java/com/example/shiftmatch/domain/EmployeeTest.java`
  - 完了条件：
    - `./mvnw test -Dtest=EmployeeTest` が成功する
    - `@DisplayName` が Given-When-Then パターンの日本語で書かれている
    - `Wish` に `DESIRED`／`AVAILABLE`／`UNAVAILABLE` の 3 定数が存在する

- [ ] **T2. 割り当て結果モデル（AssignmentResult record）と Service の雛形、および基本の成立ケースを実装する**
  - 依頼事項：
    - `AssignmentResult` record を作成する。フィールドは `List<Employee> earlyEmployees`（早番 2 名）、`List<Employee> lateEmployees`（遅番 2 名）、`int score`（0〜4）、`List<Employee> unassignedEmployees`（未割り当て）とする（`docs/specifications.md` 7 章 出力仕様）
    - `ShiftAssignmentService` インタフェースを作成し、`Optional<AssignmentResult> assign(List<Employee> employees)` を宣言する
    - `ShiftAssignmentServiceImpl` を作成し、まず「有効な従業員がちょうど 4 名で、全員が早番・遅番ともに `AVAILABLE`（○）のとき、入力順インデックスの辞書順で最初に列挙される組み合わせ（早番 = 先頭 2 名、遅番 = 残り 2 名）を返す」という最も基本的な振る舞いをテストファーストで実装する
    - この Todo で、早番 2 名の組を入力順インデックスの辞書順（(0,1), (0,2), …）で列挙し、各組について残りの従業員から遅番 2 名の組を同じ順序で列挙する、という全探索の骨格を作る（`docs/specifications.md` 5.3 節・5.4 節）。スコア計算・× の除外・同点更新は後続の Todo（T3〜T6）で追加するため、このテストケースでは考慮不要
  - 対象ファイル：
    - `src/main/java/com/example/shiftmatch/domain/AssignmentResult.java`
    - `src/main/java/com/example/shiftmatch/service/ShiftAssignmentService.java`
    - `src/main/java/com/example/shiftmatch/service/ShiftAssignmentServiceImpl.java`
    - `src/test/java/com/example/shiftmatch/service/ShiftAssignmentServiceImplTest.java`
  - 完了条件：
    - `./mvnw test -Dtest=ShiftAssignmentServiceImplTest` が成功する
    - `@DisplayName` の先頭に `[F-3]` が付いている
    - テストは、4 名全員が両枠 `AVAILABLE` のとき、早番に入力順の先頭 2 名、遅番に残り 2 名が割り当てられることを検証している

- [ ] **T3. H-3（1 人 1 枠まで）を明示的に検証するテストを追加する**
  - 依頼事項：
    - 5 名以上（例：5 名、全員両枠 `AVAILABLE`）の入力で、算出された `AssignmentResult` の `earlyEmployees` と `lateEmployees` に同一の従業員が重複して含まれないことを検証するテストを追加する
    - T2 の全探索骨格（早番を決めた後、残りの従業員から遅番を選ぶ）で既に満たされている場合は、プロダクションコードの変更は不要でよい。ただし先にテストを書き、GREEN であることを確認すること
  - 対象ファイル：
    - `src/test/java/com/example/shiftmatch/service/ShiftAssignmentServiceImplTest.java`
  - 完了条件：
    - `./mvnw test -Dtest=ShiftAssignmentServiceImplTest` が成功する
    - `@DisplayName` の先頭に `[H-3]` が付いている
    - テストは、早番と遅番の割り当て従業員に重複がないことをアサーションで確認している

- [ ] **T4. H-4（× 申告の枠には割り当てない）を実装する**
  - 依頼事項：
    - ある従業員の早番希望が `UNAVAILABLE`（×）のとき、その従業員が早番の案に含まれないことを検証するテストを先に書き、RED を確認する
    - 同様に、遅番希望が `UNAVAILABLE` の従業員が遅番の案に含まれないことを検証するテストも書く
    - 組み合わせ生成時に、対象の枠を × と申告した従業員を候補から除外する実装を追加する
  - 対象ファイル：
    - `src/main/java/com/example/shiftmatch/service/ShiftAssignmentServiceImpl.java`
    - `src/test/java/com/example/shiftmatch/service/ShiftAssignmentServiceImplTest.java`
  - 完了条件：
    - `./mvnw test -Dtest=ShiftAssignmentServiceImplTest` が成功する
    - `@DisplayName` の先頭に `[H-4]` が付いている
    - 早番 × の従業員が早番案に含まれないケース、遅番 × の従業員が遅番案に含まれないケースの両方がテストされている

- [ ] **T5. スコア計算と最大化を実装する（F-3）**
  - 依頼事項：
    - スコア＝「割り当てられた 4 名のうち、その枠を `DESIRED`（◎）と申告していた人数」（0〜4）を計算するテストを先に書く（`docs/specifications.md` 5.2 節）
    - 複数の組み合わせが存在するケース（例：5〜6 名で、ある 1 組だけが ◎ をより多く含む）で、スコアが最大の組み合わせが採用されることを検証するテストを書く
    - 全探索したすべての組み合わせのスコアを計算し、それまでの最大スコアより大きい場合のみ採用候補を更新する実装を追加する（比較は `>`。同点で更新しないことは T6 で検証する）
  - 対象ファイル：
    - `src/main/java/com/example/shiftmatch/service/ShiftAssignmentServiceImpl.java`
    - `src/test/java/com/example/shiftmatch/service/ShiftAssignmentServiceImplTest.java`
  - 完了条件：
    - `./mvnw test -Dtest=ShiftAssignmentServiceImplTest` が成功する
    - `@DisplayName` の先頭に `[F-3]` が付いている
    - `AssignmentResult.score()` が期待値と一致することを検証するテストが存在する
    - ◎ をより多く含む組み合わせが優先されるケースがテストされている

- [ ] **T6. 同点時の決定方法を検証するテストを追加する（H-3 ではなく仕様 5.3 節）**
  - 依頼事項：
    - 最大スコアの組み合わせが複数存在するケース（例：全員 `AVAILABLE` でスコアが常に 0 になるケース、または ◎ の人数が複数組で同数になるケース）で、早番の組を入力順インデックスの辞書順で列挙し、最初に最大スコアへ到達した組み合わせが採用されることを検証するテストを書く
    - 実装が `>` で比較しており `>=` になっていないことを確認する（同点では更新しない）。既に T5 の実装で満たされている場合はプロダクションコードの変更は不要でよいが、先にテストを書き GREEN を確認すること
  - 対象ファイル：
    - `src/test/java/com/example/shiftmatch/service/ShiftAssignmentServiceImplTest.java`
  - 完了条件：
    - `./mvnw test -Dtest=ShiftAssignmentServiceImplTest` が成功する
    - `@DisplayName` の先頭に `[F-3]` が付いている
    - スコアが同点になる複数の組み合わせを用意し、辞書順で最初に到達した組み合わせが採用されることを検証している

- [ ] **T7. 不成立判定を実装する**
  - 依頼事項：
    - ハード制約（H-1〜H-4）を満たす組み合わせが 1 つも存在しないケースで、`assign` が `Optional.empty()` を返すことを検証するテストを先に書く（`docs/specifications.md` 6 章）
    - 具体例として、次の 2 パターンを含める
      - 早番希望・遅番希望のいずれかが `AVAILABLE`／`DESIRED` な従業員が 4 名未満しかいない（組み合わせを作れない）ケース
      - 「早番に割り当て可能な 2 名」と「遅番に割り当て可能な 2 名」が同一の 2 名であるため、枠ごとに独立に数えると成立しそうに見えるが、実際には H-3 のため不成立になるケース（仕様書 6 章に明記された例）
    - 総当たりで成立する組み合わせが 0 件だった場合に `Optional.empty()` を返す実装を追加する（枠ごとの可能人数を独立に数える判定方法は使わないこと）
  - 対象ファイル：
    - `src/main/java/com/example/shiftmatch/service/ShiftAssignmentServiceImpl.java`
    - `src/test/java/com/example/shiftmatch/service/ShiftAssignmentServiceImplTest.java`
  - 完了条件：
    - `./mvnw test -Dtest=ShiftAssignmentServiceImplTest` が成功する
    - `@DisplayName` の先頭に `[F-3]` が付いている
    - 上記 2 パターンの不成立ケースがそれぞれテストされ、`assign` が空の `Optional` を返すことを確認している

- [ ] **T8. 未出勤者一覧を実装する（出力仕様 7 章）**
  - 依頼事項：
    - 5 名以上（例：5 名）の入力で成立するケースにおいて、`AssignmentResult.unassignedEmployees()` に、早番にも遅番にも割り当てられなかった従業員が含まれることを検証するテストを先に書く
    - 割り当て結果から未割り当ての従業員を算出し、`AssignmentResult` に含める実装を追加する（T2 で未実装のまま残していた場合はここで実装する）
  - 対象ファイル：
    - `src/main/java/com/example/shiftmatch/service/ShiftAssignmentServiceImpl.java`
    - `src/test/java/com/example/shiftmatch/service/ShiftAssignmentServiceImplTest.java`
  - 完了条件：
    - `./mvnw test -Dtest=ShiftAssignmentServiceImplTest` が成功する
    - `@DisplayName` の先頭に `[F-3]` が付いている
    - `unassignedEmployees()` の内容を検証するテストが存在する

- [ ] **T9. 全体テストの成功を確認する**
  - 依頼事項：
    - `./mvnw test` を実行し、Spotless・Checkstyle を含めて全テストが成功することを確認する
    - 未整形箇所があれば `./mvnw spotless:apply` を実行してから再確認する
    - Javadoc 未記載の public/protected/package-private なクラス・メソッドがないか確認する（`.agents/rules/javadoc.md`）
  - 対象ファイル：なし（確認のみ）
  - 完了条件：
    - `./mvnw test` が成功する（テスト失敗・Spotless 違反・Checkstyle 違反のいずれもない）

## 実行ログ

<!-- implementer が試行結果（失敗理由・リトライ回数）を追記する欄。作成時は空のままにする -->
