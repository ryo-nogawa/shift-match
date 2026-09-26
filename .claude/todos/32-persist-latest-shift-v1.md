# Todo: 従業員入力と決定シフトを最新 1 件として保存・復元する

- Issue: #32
- ブランチ: feature/32-persist-latest-shift
- 版: v1
- 対象仕様: Issue #32 の記載（仕様 ID は未割り当て。要求定義・要件定義は変更しない）。既存の H-1〜H-3、V-1〜V-5 の挙動は変えない
- 作成日: 2026-09-26

## 前提

- 読むべきドキュメント・規約：Issue #32（`gh issue view 32`）、`.claude/rules/tdd.md`、`.agents/rules/test.md`、`.agents/rules/config.md`、`.agents/rules/javadoc.md`、`.agents/rules/comment.md`、`.agents/rules/naming.md`、`.agents/rules/exception.md`
- 追加する依存は `spring-boot-starter-data-jdbc` と `h2` の 2 つだけ（ユーザー承認済み）。JPA・Flyway など他の依存は追加しない。Spring Boot 4.1.1 の API に迷ったら公式ドキュメントを確認する
- 設計方針（この構成で実装する）
  1. 永続化は `com.example.shiftmatch.persistence` パッケージの `LatestShiftRepository`（クラス、`@Repository`）に集約する。実装は Spring の `JdbcClient` を使う（Spring Data のリポジトリインターフェースは使わない）。保存は `@Transactional` で「全削除 → 挿入」とし、常に最新 1 件だけを保持する
  2. テーブルは `src/main/resources/schema.sql` に `CREATE TABLE IF NOT EXISTS` で定義し、`spring.sql.init.mode=always` で起動時に作る
     - `saved_employee(row_index INT PRIMARY KEY, name VARCHAR(255) NOT NULL, off BOOLEAN NOT NULL, start_time TIME NULL, end_time TIME NULL)`：従業員入力（名前が空の行は保存しない。`row_index` は 0 からの連番）
     - `saved_assignment(assignment_index INT PRIMARY KEY, employee_name VARCHAR(255) NOT NULL, slot VARCHAR(16) NOT NULL, break_start TIME NOT NULL, break_end TIME NOT NULL)`：決定した割り当て（`slot` は `ShiftSlot` の列挙子名。`assignments()` の順）
     - `saved_score(id INT PRIMARY KEY CHECK (id = 1), score INT NOT NULL)`：ずれの合計（常に `id = 1` の 1 行）
     - `off` は H2 の予約語ではないが、問題があれば列名を `is_off` に変えてよい
  3. 保存の入口は `void save(List<Employee> employees, Optional<AssignmentResult> result)`。`employees` は名前が空でない従業員（入力順）。`result` が空（不成立）のときは、従業員入力だけを保存し、以前の割り当て・スコアは消す。読み出しの入口は `List<Employee> findEmployees()`（`row_index` の昇順。保存がなければ空リスト）。割り当て・スコアの読み出し API は今回は作らない（テストは `JdbcClient` で表を直接検証する）
  4. 接続設定は `application.properties` に書く（`.agents/rules/config.md`）：`spring.datasource.url=jdbc:h2:file:./data/shift-match`（`data/` は `.gitignore` 済み）、`spring.sql.init.mode=always`。テスト用に `src/test/resources/application.properties` を作り、`spring.application.name=shift-match`、`spring.datasource.url=jdbc:h2:mem:shift-match-test;DB_CLOSE_DELAY=-1`、`spring.sql.init.mode=always` を書く（テストがファイル DB を汚さないようにする）
  5. コントローラーは `LatestShiftRepository` をコンストラクタで受け取る。`ShiftControllerTest`（`@WebMvcTest`）には `@MockitoBean private LatestShiftRepository latestShiftRepository;` を追加する（既存テストの期待値は変えない）
  6. 復元：`GET /` で `findEmployees()` の結果を `EmployeeForm` に詰める（開始・終了は `HH:mm` 文字列、休みの行は開始・終了を空文字にする）。行数が 12 未満なら空の `EmployeeForm` で 12 行まで補う。保存が空なら従前どおり空 12 行。インデックスは 0 から欠番なし
  7. 保存：`POST /shift` で入力チェック（V-1〜V-5 に相当する既存の分岐）を通り、算出まで完了したときだけ保存する（エラー時は保存しない）。算出結果が不成立の場合も従業員入力は保存する
  8. 保存失敗：`org.springframework.dao.DataAccessException` を捕まえ、モデル属性 `saveError` に「保存に失敗しました。もう一度シフトを作成して保存し直してください。」を設定して画面（`index.html`）に `<section class="alert" role="alert">` で表示する。シフトの算出結果は保存に失敗しても通常どおり表示する
- 既存テストが固定している HTML・挙動は壊さない（既存テストは書き換えない）

## Todo

- [ ] **T1. H2・JDBC の依存と接続設定、スキーマを追加する**
  - 依頼事項：`pom.xml` に `spring-boot-starter-data-jdbc`（compile）と `h2`（`runtime` スコープ）を追加する。`application.properties` に接続設定（前提の設計方針 4）、`src/main/resources/schema.sql` に 3 テーブル（設計方針 2）、`src/test/resources/application.properties` を追加する。テストを先に書く：`@SpringBootTest` で `JdbcClient` を注入し、3 テーブルが存在し `SELECT COUNT(*)` が 0 を返すことを検証する（RED：テーブルがなくアサーション/例外で失敗 → GREEN）
  - 対象ファイル：`pom.xml`、`src/main/resources/application.properties`、`src/main/resources/schema.sql`、`src/test/resources/application.properties`、`src/test/java/com/example/shiftmatch/persistence/SchemaTest.java`
  - 完了条件：
    - テストを先に書き、RED（テーブルなし）を確認した
    - `./mvnw test -Dtest=SchemaTest` と `./mvnw test -Dtest=ShiftMatchApplicationTests` が成功する
    - `pom.xml` の依存追加が上記 2 つだけである（`git diff pom.xml` で確認）
- [ ] **T2. 従業員入力を保存して読み出せる**
  - 依頼事項：`LatestShiftRepository`（`com.example.shiftmatch.persistence`）に `save(List<Employee>, Optional<AssignmentResult>)` と `findEmployees()` を作る。この Todo では従業員入力の保存・読み出しだけを実装する（`result` の中身は使わない）。`Employee` の 4 項目（名前・休み・開始・終了）が入力順のまま往復できること、休みの行は開始・終了が `null` で返ること、保存がなければ空リストを返すことをテストする
  - 対象ファイル：`src/main/java/com/example/shiftmatch/persistence/LatestShiftRepository.java`、`src/test/java/com/example/shiftmatch/persistence/LatestShiftRepositoryTest.java`
  - 完了条件：
    - テストを先に書き、アサーションで RED になることを確認した
    - `@DisplayName` に日本語で「従業員入力を保存して読み出せる」「保存がなければ空」を含むテストがある。各テストの前に 3 表を全削除する（`@BeforeEach`）
    - `./mvnw test -Dtest=LatestShiftRepositoryTest` が成功する
- [ ] **T3. 2 回目の保存は最新データで上書きされる**
  - 依頼事項：既存データがある状態で `save` を再度呼ぶと、例外なく、1 回目のデータが残らず 2 回目のデータだけが読み出されることを、テストで検証する（1 回目 3 名 → 2 回目 2 名で、`findEmployees()` が 2 名だけ返す）。必要なら `save` を `@Transactional` にして「全削除 → 挿入」にする
  - 対象ファイル：`LatestShiftRepository.java`、`LatestShiftRepositoryTest.java`
  - 完了条件：
    - テストを先に書き、RED を確認した（すでに通る場合は、その旨を実行ログに書き、行数が減る場合のテストを足して RED を確認する）
    - `./mvnw test -Dtest=LatestShiftRepositoryTest` が成功する
- [ ] **T4. 決定したシフト（割り当てとスコア）を保存し、不成立のときは消す**
  - 依頼事項：`result` が存在するとき、`saved_assignment` に 8 行（`assignments()` の順、`slot` は列挙子名、休憩開始・終了）、`saved_score` に 1 行（`score`）を保存する。`result` が空のときは 2 表を空にする（従業員入力は保存する）。上書き保存で 8 行・1 行のまま増えないことも検証する。テストでは `AssignmentResult` を直接組み立て（`ShiftSlot.totalEmployees()` 件の `ShiftAssignment`）、`JdbcClient` で表を検証する
  - 対象ファイル：`LatestShiftRepository.java`、`LatestShiftRepositoryTest.java`
  - 完了条件：
    - テストを先に書き、RED を確認した
    - 「保存すると 8 件と得点が入る」「2 回保存しても 8 件のまま」「不成立（空）で割り当て・得点が消え、従業員入力は残る」の 3 つのテストがある
    - `./mvnw test -Dtest=LatestShiftRepositoryTest` が成功する
- [ ] **T5. 画面表示時に保存済みの従業員入力を復元する**
  - 依頼事項：`ShiftController` に `LatestShiftRepository` を注入し、`GET /` で設計方針 6 のとおり復元する。`ShiftControllerTest` に `@MockitoBean LatestShiftRepository` を追加する。テスト：(1) 保存済み 2 名（1 名は休み）のスタブで `GET /` すると、モデルの `shiftForm` の行数が 12、先頭 2 行に名前・休み・`HH:mm` の開始・終了が入り、休みの行の開始・終了は空文字、残りは空行である (2) 保存が空（`List.of()`）のとき従前どおり空 12 行
  - 対象ファイル：`src/main/java/com/example/shiftmatch/controller/ShiftController.java`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - テストを先に書き、アサーションで RED を確認した
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する（既存テストも含めて全件）
- [ ] **T6. シフト算出後に従業員入力と結果を保存する**
  - 依頼事項：`POST /shift` で、入力エラーがなく算出まで完了したときに `latestShiftRepository.save(validEmployees, result)` を呼ぶ（`result` は `Optional<AssignmentResult>`。不成立は `Optional.empty()`）。入力エラー（重複氏名・時間帯エラー・13 名以上）のときは `save` を呼ばない。テスト：成功時に `save` が呼ばれ引数の従業員が入力順、結果が渡した `AssignmentResult` と一致する（`ArgumentCaptor`）／不成立時に `Optional.empty()` で呼ばれる／入力エラー時に `never()` で呼ばれない
  - 対象ファイル：`ShiftController.java`、`ShiftControllerTest.java`
  - 完了条件：
    - テストを先に書き、RED を確認した
    - 上記 3 つのテストがあり、`./mvnw test -Dtest=ShiftControllerTest` が成功する
- [ ] **T7. 保存に失敗したらエラーとリトライを促す文言を表示する**
  - 依頼事項：設計方針 8 のとおり、`save` が `DataAccessException`（テストでは `new org.springframework.dao.DataAccessResourceFailureException("test")`）を投げたとき、`saveError` をモデルに設定し、`index.html` に `<section class="alert" role="alert" th:if="${saveError != null}">` で表示する。算出結果（`assignmentResult`）は保存失敗でも表示され、HTTP ステータスは 200。保存成功時は `saveError` を出さない。テスト：失敗時にモデルの `saveError` が文言と一致し、レスポンス本文に文言が含まれ、`assignmentResult` もある／成功時は `saveError` が null
  - 対象ファイル：`ShiftController.java`、`src/main/resources/templates/index.html`、`ShiftControllerTest.java`
  - 完了条件：
    - テストを先に書き、RED を確認した
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
- [ ] **T8. 全体を確認する**
  - 依頼事項：`./mvnw spotless:apply` で整形し、`./mvnw test` を実行する。失敗したら原因を直す。`README.md` に「最新 1 件を H2（`./data/`）に保存・復元する」旨を 1〜2 行で追記する（仕様書 `docs/` は変更しない）
  - 対象ファイル：`README.md`（他は必要な修正のみ）
  - 完了条件：
    - `./mvnw test` で全テストが成功し、Spotless・Checkstyle の違反が 0 件である
    - `git diff --stat main -- docs` が空である
    - `git status` で `data/` や `target/` がコミット対象になっていない

## 実行ログ

<!-- implementer が試行結果（失敗理由・リトライ回数）を追記する欄。作成時は空のままにする -->
