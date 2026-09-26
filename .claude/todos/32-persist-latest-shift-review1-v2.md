# Todo: #32 Codex レビュー 1 ラウンド目の指摘修正

- Issue: #32
- ブランチ: feature/32-persist-latest-shift
- 版: v2
- 対象仕様: Issue #32 の記載（要求定義・要件定義は変更しない）。既存の H-1〜H-3、V-1〜V-5 の挙動は変えない
- 作成日: 2026-09-26

## 前提

- **前回（v1）の失敗理由**：T3 で、`EmployeeForm.name` の `@Size(max = 255)`・`ShiftController#toNameErrors`・`InvalidNameError`（`domain`）まで実装したが、`index.html` に氏名エラーの表示欄がなく、エラー文言が本文に出ないため T3 のテストが失敗した（作業ツリーに未コミットの実装が残っている）
- **今回（v2）の変更点**：T3 を T3a（保存抑止）と T3b（画面表示）に分割した。T3b は `index.html` の `timeRangeErrors` の `<section class="alert" ...>`（`th:if="${timeRangeErrors != null && ...}"`）の直後に、同じ形の `<section class="alert" role="alert" th:if="${nameErrors != null && !nameErrors.isEmpty()}">` を追加する。中身は `<h2>入力エラー</h2>` と、`th:each="error : ${nameErrors}"` の `<li><span th:text="|${error.rowIndex() + 1}行目 ${error.message()}|"></span></li>`。`InvalidNameError` は `rowIndex()` と `message()` を持つレコード。作業ツリーに残っている未コミットの変更（`EmployeeForm.java`、`ShiftController.java`、`ShiftControllerTest.java`、`InvalidNameError.java`）は捨てず、T3a・T3b のコミットに分けて使う

- 読むべきもの：`.claude/todos/32-persist-latest-shift-v1.md`（元の設計方針）、`target/reviews/code-quality-review.md`、`target/reviews/security-risk-review.md`、`.claude/rules/tdd.md`、`.agents/rules/test.md`、`.agents/rules/exception.md`、`.agents/rules/comment.md`、`.agents/rules/javadoc.md`
- **新しい依存は追加しない。`docs/`・`AGENTS.md` は変更しない**（Issue #32 が「要求定義・要件定義は修正せず、Issue の記載を仕様とする」と定めている。`pom.xml` も変更しない）
- ユーザー決定：(1) CSRF 対策は Origin / Sec-Fetch-Site 検証（依存追加なし）、(2) 氏名に最大 255 文字の入力チェックを追加する
- テスト規約（`.agents/rules/test.md`）：`@Nested` でグループ化し、`@DisplayName` は日本語の `Given: ...、When: ...、Then: ...` 形式にする。各テストは実行順に依存させず `@BeforeEach` で初期化する。既存テストの書き換えは、この Todo で対象になっている新規テスト（`SchemaTest`、`LatestShiftRepositoryTest`、`ShiftControllerTest` のうち #32 で追加した保存・復元・保存失敗のテスト）に限る
- 実装上の注意
  - CSRF：`POST /shift` だけを対象にする `HandlerInterceptor`（`com.example.shiftmatch.config.SameOriginInterceptor`）と、それを登録する `WebMvcConfigurer`（`com.example.shiftmatch.config.WebConfig`、`@Configuration`）を作る。判定：`Sec-Fetch-Site` ヘッダがあれば `same-origin` または `none` のときだけ許可。なければ `Origin` ヘッダがあるとき、その `host[:port]` がリクエストの `Host` ヘッダと一致するときだけ許可。どちらのヘッダもなければ許可（ブラウザ以外・旧ブラウザ）。拒否時は HTTP 403 を返し、保存しない。`@WebMvcTest` でもインターセプターが効くか確認する（効かない場合は `@Import(WebConfig.class)` を付ける）。既存テストは POST にヘッダを付けないので許可され、そのまま通ること
  - 氏名の入力チェック：`EmployeeForm.name` に `@Size(max = 255, message = "...")` を付ける。**現在のコントローラーは `bindingResult` のうち `start`/`end` のエラーしか画面に出さない**ため、`name` のエラーが無視されないよう、エラーがあれば保存せず `index` を再表示し、氏名の行番号とメッセージ（例：「N 行目の氏名は 255 文字以内で入力してください。」）を画面に出す。メッセージは既存の `timeRangeErrors` と同様のエラー表示（`alert` セクション）に揃える。空の氏名の行は従来どおり除外（V-1）

## Todo

- [x] **T1. `save` を 1 つのトランザクションにし、途中失敗でロールバックされる**
  - 依頼事項：`LatestShiftRepository#save` に `org.springframework.transaction.annotation.Transactional` を付け、「全削除 → 全挿入」を単一トランザクションにする。テストを先に書く：既存データ（従業員 2 名）を保存した状態で、256 文字の氏名を含む従業員リストを `save` すると `DataAccessException` が出て、旧データ（2 名、割り当て・スコア）がそのまま残ること（RED：今はトランザクションがなく旧データが消える）
  - 対象ファイル：`src/main/java/com/example/shiftmatch/persistence/LatestShiftRepository.java`、`src/test/java/com/example/shiftmatch/persistence/LatestShiftRepositoryTest.java`
  - 完了条件：
    - テストを先に書き、旧データが消えて RED になることを確認した
    - `./mvnw test -Dtest=LatestShiftRepositoryTest` が成功する
- [x] **T2. 保存失敗の例外をログに出す**
  - 依頼事項：`ShiftController` の `DataAccessException` を捕まえる箇所で、SLF4J（`LoggerFactory.getLogger(ShiftController.class)` の `private static final Logger`）を使い、`LOGGER.error("最新シフトの保存に失敗しました", e)` の形でスタックトレース付きで記録してから、既存の画面表示を行う（`.agents/rules/exception.md`）。テスト：保存失敗時にログが出ることを、`ListAppender`（Logback。`ch.qos.logback.core.read.ListAppender`）を `ShiftController` のロガーに付けて、ERROR レベルのイベントが 1 件あり `getThrowableProxy()` が null でないことで検証する
  - 対象ファイル：`ShiftController.java`、`ShiftControllerTest.java`
  - 完了条件：
    - テストを先に書き、RED を確認した
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
- [ ] **T3a. 氏名が 255 文字を超えたら入力エラーにして保存しない**
  - 依頼事項：作業ツリーの未コミット実装（`@Size(max = 255)`、`toNameErrors`、`InvalidNameError`）を使い、氏名が 256 文字のとき `POST /shift` で `save` も `assign` も呼ばれず（`never()`）、`index` が表示され、モデルの `nameErrors` に行番号（0 始まり）とメッセージが入ることを検証する。255 文字ちょうど・氏名が空の行（V-1）はエラーにならず従来どおり処理される。`toNameErrors` 内の `Pattern.compile` は、`TIME_RANGE_FIELD_PATTERN` と同様に `private static final Pattern NAME_FIELD_PATTERN` に切り出す。HTML 本文の検証はこの Todo に含めない（T3b）
  - 対象ファイル：`EmployeeForm.java`、`ShiftController.java`、`src/main/java/com/example/shiftmatch/domain/InvalidNameError.java`、`ShiftControllerTest.java`
  - 完了条件：
    - 256 文字で `save` が呼ばれない・`nameErrors` が入るテストと、255 文字・空の氏名がエラーにならないテストがある
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する（本文検証のテストは T3b で追加するので、この時点では含めない）
- [ ] **T3b. 氏名のエラーを画面に表示する**
  - 依頼事項：前提「今回（v2）の変更点」のとおり `index.html` に氏名エラーの表示欄を追加する。テストを先に書く：256 文字の氏名で `POST /shift` したレスポンス本文に「1行目」と「氏名は255文字以内で入力してください。」が含まれる（RED：欄がなく含まれない）。エラーがないときは本文にそのメッセージが含まれない
  - 対象ファイル：`src/main/resources/templates/index.html`、`ShiftControllerTest.java`
  - 完了条件：
    - テストを先に書き、RED を確認した
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する（既存テストも全件）
- [ ] **T4. 別オリジンからの `POST /shift` を拒否する**
  - 依頼事項：前提の「CSRF」のとおり `SameOriginInterceptor` と `WebConfig` を実装する。テスト（`ShiftControllerTest` の `@Nested` として、または `SameOriginInterceptorTest` を別に作る）：(1) `Sec-Fetch-Site: cross-site` → 403 で `save` も `assign` も呼ばれない (2) `Sec-Fetch-Site: same-origin` → 200 (3) `Origin: http://evil.example` と `Host: localhost:8080` → 403 (4) `Origin: http://localhost:8080` と `Host: localhost:8080` → 200 (5) ヘッダなし → 200 (6) `GET /` は `Sec-Fetch-Site: cross-site` でも 200
  - 対象ファイル：`src/main/java/com/example/shiftmatch/config/SameOriginInterceptor.java`、`src/main/java/com/example/shiftmatch/config/WebConfig.java`、テストファイル
  - 完了条件：
    - テストを先に書き、RED を確認した（403 になるべき場面が 200 で失敗する）
    - `./mvnw test -Dtest=ShiftControllerTest` と、作成したテストクラスが成功する
- [ ] **T5. 保存・復元テストの検証を強化する**
  - 依頼事項：(a) `ShiftControllerTest` の復元テストを、モデルの `ShiftForm` を取り出して、全 12 行・各行の名前／休み／開始／終了（休みの行は開始・終了が空文字）を検証する形にする (b) `LatestShiftRepositoryTest` の割り当て保存テストを、`saved_assignment` を `assignment_index` 順に読み、従業員名・枠（列挙子名）・休憩開始／終了・順序を期待値と比較する形にする（行数とスコアだけの検証をやめる）
  - 対象ファイル：`ShiftControllerTest.java`、`LatestShiftRepositoryTest.java`
  - 完了条件：
    - `./mvnw test -Dtest=LatestShiftRepositoryTest` と `./mvnw test -Dtest=ShiftControllerTest` が成功する
    - 保存の列（例：`slot`）を実装側でわざと入れ替えるとテストが失敗することを確認し、確認後に元へ戻した（実行ログに書く）
- [ ] **T6. 新規テストを規約に合わせる**
  - 依頼事項：`SchemaTest`、`LatestShiftRepositoryTest`、`ShiftControllerTest` の #32 で追加した部分を、`@Nested`（正常系・異常系など）と日本語 `Given/When/Then` 形式の `@DisplayName` に直す。`SchemaTest` は、他のテストが残したデータに依存しないよう、`@BeforeEach` で 3 表を全削除してから検証する（または存在確認を「空であること」と分離する）。振る舞いは変えない
  - 対象ファイル：上記 3 つのテストクラス
  - 完了条件：
    - 3 つのテストクラスの #32 追加分の `@DisplayName` がすべて `Given: ...、When: ...、Then: ...` の形になっている（`grep -n "DisplayName" <ファイル>` で確認）
    - `SchemaTest` が `@BeforeEach` で初期化している
    - `./mvnw test` で全テストが成功する
- [ ] **T7. 自明なコメントを削除し、README を整合させる**
  - 依頼事項：(a) `LatestShiftRepository`・`ShiftController` の、コードから明らかな「何をしているか」だけのコメント（「全削除」「従業員入力を挿入」「入力エラーがなく算出まで完了したときに保存」など）を削除する。残すコメントは「なぜ」（例：最新 1 件を置換するため、入力エラー時に前回の保存を維持するため）だけにする (b) `README.md` の「データベースは使用しない」（17・23 行目付近）と、H2 に保存・復元する旨の記述（19 行目付近）の矛盾を解消する。保存するのは従業員入力とシフト結果の最新 1 件で、画面表示時に復元されるのは従業員入力のみ（結果は復元しない）と書く。`docs/`・`AGENTS.md` は変更しない
  - 対象ファイル：`LatestShiftRepository.java`、`ShiftController.java`、`README.md`
  - 完了条件：
    - `README.md` に「データベースは使用しない」という記述が残っていない（`grep -n "データベース" README.md` で確認）
    - `git diff --stat main -- docs AGENTS.md pom.xml` が空である
    - `./mvnw spotless:apply` の後、`./mvnw test` で全テストが成功し、Spotless・Checkstyle の違反が 0 件である

## 実行ログ

<!-- implementer が試行結果（失敗理由・リトライ回数）を追記する欄。作成時は空のままにする -->
