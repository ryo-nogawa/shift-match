# Todo: Controller層と画面に F-1〜F-5・V-3 を実装する

- Issue: #8
- ブランチ: feature/8-controller-screen
- 版: v1
- 対象仕様: F-1, F-2, F-3, F-4, F-5, V-2（表示のみ）, V-3
- 作成日: 2026-09-24

## 前提

- 読むべきドキュメント・規約：
  - `docs/specifications.md`（4 章 入力仕様、5〜7 章 割り当て・出力仕様、8 章 画面仕様）
  - `.claude/rules/tdd.md`（Red → Green → Refactor を厳守）
  - `.agents/rules/test.md`（`@Nested` ＋ `@DisplayName`（Given-When-Then）、仕様 ID を `[F-1]` 形式で先頭に付与、AssertJ・Hamcrest 禁止、標準 Assertions（JUnit5）と Mockito のみ）
  - `.agents/rules/naming.md`（クラス UpperCamelCase、実装クラスはインタフェース名 + `Impl`）
  - `.agents/rules/javadoc.md`（public/protected/package-private に Javadoc 必須、private とテストコードは対象外）
  - `.agents/rules/lambda.md`（メソッド参照 `Class::method` 禁止、ラムダ式を使う）
  - `.agents/rules/exception.md`（チェック例外のみ try-catch。`IllegalArgumentException` などの非チェック例外を制御フローとして捕捉しない）
  - `.agents/rules/formatting.md`（`./mvnw spotless:apply` で整形してからコミット）
- 設計方針（既存コードとの整合性を優先する）：
  - **画面はサーバー側レンダリング（Thymeleaf）**。Controller は `com.example.shiftmatch.controller` パッケージに置く：`ShiftController`（`@Controller`）、`ShiftForm`（`List<EmployeeForm> employees` を持つ）、`EmployeeForm`（`String name`, `String earlyWish`, `String lateWish` を持つ）。いずれも通常の Java クラス（getter/setter 付き、no-args コンストラクタ）とする。Spring MVC のフォームバインディングは setter 経由で行う
  - **早番・遅番希望の値は `Wish` enum の定数名（`DESIRED` / `AVAILABLE` / `UNAVAILABLE`）を `<select>` の value として送信する**（◎／○／× は表示ラベルのみ）。理由：Unicode 記号をそのまま value にすると文字コード起因の不具合リスクがあり、enum 名なら判定・変換が単純になるため
  - **V-3 の判定・`String → Wish` 変換は、例外を使わずに行う**：`Arrays.stream(Wish.values()).anyMatch(w -> w.name().equals(raw))` のような列挙型の走査で判定する。`Wish.valueOf(raw)` を try-catch で囲む実装は `.agents/rules/exception.md` 違反のため禁止
  - **入力チェックの順序**：V-1（氏名空行の除外）→ V-2（氏名重複）→ V-3（希望値の不正）→ V-4（有効従業員4名未満は不成立）。V-2 と V-3 は **どちらもチェックしてからまとめてエラー表示する**（1 件目のエラーだけで打ち切らない。ユーザーが 1 往復で全エラーを確認できるようにするため）。V-2・V-3 のいずれかにエラーがあれば `ShiftAssignmentService#assign` は呼び出さない
  - **氏名が空の行（V-1 対象）は V-3 の判定対象外**：早番・遅番希望が未入力でもエラーにしない
  - **`ShiftAssignmentService#findDuplicateNames` と `#assign` には、画面から送信された全行（氏名が空の行を含む）を `Employee` に変換したリストをそのまま渡す**（両メソッドとも内部で氏名が空の行を除外する実装が既にあるため、Controller 側で事前フィルタしない）
  - **氏名が空の行・V-3 で不正だった行の `Wish` 変換**：変換できない場合は例外を投げず、その行には便宜上 `Wish.UNAVAILABLE` を設定してよい（V-3 エラーがあれば `assign` を呼ばないため、この値が算出結果に影響することはない）
  - **初期表示（GET `/`）では 4 行分の空の `EmployeeForm` を用意する**（成立に必要な最少人数が 4 名のため）。行追加後の削除機能は今回のスコープに含めない（`docs/specifications.md` 3 章の機能一覧に削除は含まれていない）
  - **F-2（行追加）の JavaScript**：`src/main/resources/static/js/shift-form.js` に実装する。追加行の `name` 属性インデックスは、追加時点の行数（0 始まり）をそのまま使う（削除機能がないためインデックスは常に連番で欠番が出ない）
  - **テンプレート**：`src/main/resources/templates/index.html` に 1 画面で実装する（`docs/specifications.md` 8 章）
  - **Controller のテストは `@WebMvcTest(ShiftController.class)` ＋ `MockMvc` を使う**。`ShiftAssignmentService` は `org.springframework.test.context.bean.override.mockito.MockitoBean`（`@MockitoBean`、Spring Boot 4.1.1 系の現行アノテーション。**`@MockBean` は使わない**）でモック化する
  - **重要：pom.xml は変更しない**。`spring-boot-starter-webmvc-test`・`spring-boot-starter-thymeleaf-test`・`spring-boot-starter-validation-test` は Spring Boot 4 系の正式なテスト用スターターであり、Maven Central に存在し、ローカルリポジトリにも解決済み。`spring-boot-starter-test`（Spring Boot 3 系までの単一スターター）は本プロジェクトでは使わない
  - **Spring Boot 4 系でのパッケージ移動に注意**：`@WebMvcTest` は Spring Boot 3 系までの `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest` ではなく、**`org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`** に移動している（`spring-boot-webmvc-test` アーティファクトに含まれる）。必要であれば `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` も同じパッケージ。`MockMvc` 本体（`org.springframework.test.web.servlet.MockMvc`）と `@MockitoBean`（`org.springframework.test.context.bean.override.mockito.MockitoBean`）は `spring-test` にあり、パッケージは変更なし。テストクラスの import は次を使う：
    ```java
    import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
    import org.springframework.test.web.servlet.MockMvc;
    import org.springframework.test.context.bean.override.mockito.MockitoBean;
    import org.springframework.beans.factory.annotation.Autowired;
    ```
  - **MockMvc のレスポンス本文の検証は Hamcrest matcher（`content().string(containsString(...))`）を使わず**、`mvcResult.getResponse().getContentAsString()` で文字列を取得し、`org.junit.jupiter.api.Assertions.assertTrue(body.contains("..."))` で検証する（`.agents/rules/test.md` の「AssertJ などのアサーション用ライブラリは追加導入しない」を Hamcrest にも適用するため）
  - **不成立メッセージ（F-5・T-3 決定済み）**：原因分析を行わず、「条件を満たす組み合わせが見つかりませんでした。」という事実のみのメッセージを固定文言で表示する
  - **結果表示（F-4・T-2 決定済み）**：早番・遅番の氏名、スコア、未出勤者一覧を表形式（HTML `<table>`）で表示する。時間軸のバー表示は実装しない
  - 各 Todo の実装後、対象クラスのテストだけでなく `./mvnw test` 全体も実行し、既存テストを壊していないか確認すること
  - JavaScript（F-2 の行追加）はブラウザでのみ動作確認できる。`./mvnw test` の対象外であり、implementer はコードレビューとサーバー起動時の HTML 構造確認までとし、ブラウザでの動作確認はメインエージェントまたはユーザーが別途行う

## Todo

- [x] **T1. [F-1] 入力フォームの初期表示（GET `/`）を実装する**
  - 依頼事項：
    - `EmployeeForm`（`String name`, `String earlyWish`, `String lateWish`、getter/setter、no-args コンストラクタ、Javadoc）を作成する
    - `ShiftForm`（`List<EmployeeForm> employees`、getter/setter、Javadoc）を作成する
    - `ShiftController`（`@Controller`）を作成し、`GET /` で `ShiftForm` に空の `EmployeeForm` を 4 件セットして `Model` に `"shiftForm"` という属性名で追加し、ビュー名 `"index"` を返す
    - `src/main/resources/templates/index.html` に、`th:object="${shiftForm}"` を使ったフォームの雛形を作成する。`th:each` と `stat.index` を使い、`employees[0].name` のようなインデックス付き `name` 属性でバインドする（`docs/specifications.md` 8 章参照）。この時点では氏名・早番希望・遅番希望の入力欄と「シフトを作成」ボタンがあれば十分（早番・遅番希望は `<select>` で `DESIRED`/`AVAILABLE`/`UNAVAILABLE` を value とし、◎/○/×を表示ラベルとする。先頭に未選択用の空 value を用意する）
    - `ShiftControllerTest`（`@WebMvcTest(ShiftController.class)`）を先に書き、RED を確認してから実装する。`GET /` を呼び、ステータス 200・ビュー名 `"index"`・モデル属性 `"shiftForm"` が存在し、`employees` のサイズが 4 であることを検証する
    - `@DisplayName` の先頭に `[F-1]` を付ける
  - 対象ファイル：
    - `src/main/java/com/example/shiftmatch/controller/EmployeeForm.java`
    - `src/main/java/com/example/shiftmatch/controller/ShiftForm.java`
    - `src/main/java/com/example/shiftmatch/controller/ShiftController.java`
    - `src/main/resources/templates/index.html`
    - `src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
    - `@DisplayName` の先頭に `[F-1]` が付いたテストが存在する
    - `GET /` のテストが、ビュー名とモデル属性 `shiftForm`（4 行分の空行）を検証している

- [x] **T2. [F-2] 入力行を動的に追加する JavaScript を実装する**
  - 依頼事項：
    - `src/main/resources/static/js/shift-form.js` を作成する。「行を追加」ボタンのクリックで、既存の行数を数えて次のインデックスを算出し、氏名・早番希望・遅番希望の入力欄（`employees[N].name` 等）を持つ行をテーブル（または入力欄群）に追加する
    - `index.html` に「行を追加」ボタンと、行を追加する対象のコンテナ（`<tbody id="employee-rows">` 等）を用意し、`shift-form.js` を `<script>` で読み込む
    - Java の自動テスト対象外のため、`./mvnw test` の完了条件には含めない。実装後、`./mvnw spring-boot:run` でアプリを起動し、生成される HTML のうち行追加コンテナの `id` と初期行の `name` 属性インデックス（0〜3）が意図通りであることをレスポンス HTML で確認する（ブラウザでのクリック動作確認は対象外でよい）
  - 対象ファイル：
    - `src/main/resources/static/js/shift-form.js`
    - `src/main/resources/templates/index.html`
  - 完了条件：
    - `./mvnw test` が既存のテストを壊さず成功する（新規テストは不要）
    - `index.html` に「行を追加」ボタンと `shift-form.js` の読み込みが存在する
    - `shift-form.js` が、追加行の `name` 属性に欠番のないインデックスを付与するロジックになっている

- [x] **T3. [V-3] 早番・遅番希望の不正値チェックを実装する**
  - 依頼事項：
    - `InvalidWishError`（`int rowIndex`, `String wishLabel`。Javadoc 必須）を `domain` パッケージに作成する
    - `ShiftControllerTest` に、`POST /shift` で次のケースを検証するテストを先に書き、RED を確認してから実装する
      1. 氏名が入力されている行の早番希望が空文字列（未選択）→ レスポンス本文に不正エラーを示す文言が含まれる（`assertTrue(body.contains(...))` で検証。Hamcrest は使わない）
      2. 氏名が入力されている行の遅番希望が `DESIRED`/`AVAILABLE`/`UNAVAILABLE` のいずれでもない値 → 同様にエラー文言が含まれる
      3. 氏名が空の行の早番・遅番希望が空文字列 → エラーにならない（V-1 対象のため V-3 の対象外）
    - `ShiftController` に `POST /shift`（`@PostMapping("/shift")`）を追加する。`@ModelAttribute("shiftForm") ShiftForm shiftForm` を受け取り、氏名が空でない行についてのみ早番・遅番希望の値を `Arrays.stream(Wish.values()).anyMatch(...)` 等の例外を使わない方法で判定する。不正があれば `InvalidWishError` のリストを組み立て、`Model` に `"wishErrors"` として追加し、`shiftForm` も `Model` に戻したうえでビュー名 `"index"` を返す（この時点では `ShiftAssignmentService` を呼び出さない実装で構わない。呼び出しは T5 で追加する）
    - `index.html` に、`wishErrors` が空でない場合にエラー一覧（該当行番号＋`wishLabel`）を表示する部分を追加する。行番号は `rowIndex + 1` で 1 始まりに変換して表示する
    - `@DisplayName` の先頭に `[V-3]` を付ける
  - 対象ファイル：
    - `src/main/java/com/example/shiftmatch/domain/InvalidWishError.java`
    - `src/main/java/com/example/shiftmatch/controller/ShiftController.java`
    - `src/main/resources/templates/index.html`
    - `src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
    - `@DisplayName` の先頭に `[V-3]` が付いたテストが存在する
    - 上記 3 パターンがそれぞれテストされている
    - `InvalidWishError` に Javadoc が記載されている

- [ ] **T4. [V-2] 氏名重複チェックの結果を画面に反映する**
  - 依頼事項：
    - `ShiftControllerTest` に、`ShiftAssignmentService#findDuplicateNames` が空でないリストを返すよう `@MockitoBean` でスタブした場合、`POST /shift` のレスポンス本文に重複エラーを示す文言（該当行番号・氏名）が含まれ、かつ `ShiftAssignmentService#assign` が呼び出されていないこと（`Mockito.verify(service, Mockito.never()).assign(...)`）を検証するテストを先に書き、RED を確認する
    - `ShiftController#createShift` で、V-3 のチェック後（V-3 エラーの有無に関わらず）に `shiftAssignmentService.findDuplicateNames(employees)` を呼び出す。`employees` は画面から送信された全行を `Employee` に変換したリスト（氏名が空の行を含む。前提の設計方針を参照）とする。重複エラーまたは V-3 エラーのいずれかがあれば、`Model` に `"duplicateErrors"`（重複エラーのリスト）と `"wishErrors"` を追加し、`shiftForm` を戻してビュー名 `"index"` を返す（`assign` は呼び出さない）
    - `index.html` に、`duplicateErrors` が空でない場合にエラー一覧（該当行番号＋氏名）を表示する部分を追加する。行番号は 1 始まりに変換する
    - `@DisplayName` の先頭に `[V-2]` を付ける
  - 対象ファイル：
    - `src/main/java/com/example/shiftmatch/controller/ShiftController.java`
    - `src/main/resources/templates/index.html`
    - `src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
    - `@DisplayName` の先頭に `[V-2]` が付いたテストが存在する
    - 重複エラー時に `assign` が呼び出されないことを `Mockito.verify` で検証している

- [ ] **T5. [F-3] シフト算出（`assign` の呼び出し）を実装する**
  - 依頼事項：
    - `ShiftControllerTest` に、V-2・V-3 いずれのエラーもない入力で `POST /shift` を呼んだとき、`ShiftAssignmentService#assign` が 1 回呼び出されることを検証するテストを先に書き、RED を確認する（`@MockitoBean` でスタブした `assign` の戻り値は任意でよい）
    - `ShiftController#createShift` で、V-2・V-3 のいずれのエラーもない場合に `shiftAssignmentService.assign(employees)` を呼び出す。戻り値が `Optional<AssignmentResult>` として存在する場合は `Model` に `"assignmentResult"` を追加し、空の場合は `Model` に `"unassignable"`（`true`）を追加する。いずれの場合もビュー名 `"index"` を返す
    - `@DisplayName` の先頭に `[F-3]` を付ける
  - 対象ファイル：
    - `src/main/java/com/example/shiftmatch/controller/ShiftController.java`
    - `src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
    - `@DisplayName` の先頭に `[F-3]` が付いたテストが存在する
    - エラーがない場合に `assign` が呼び出されることを検証している

- [ ] **T6. [F-4] 成立時の結果を表形式で表示する**
  - 依頼事項：
    - `ShiftControllerTest` に、`assign` が具体的な `AssignmentResult`（早番 2 名・遅番 2 名・スコア・未出勤者を含む）を返すようスタブし、`POST /shift` のレスポンス本文に早番・遅番の氏名、スコア、未出勤者の氏名がそれぞれ含まれることを検証するテストを先に書き、RED を確認する
    - `index.html` に、`assignmentResult` が存在する場合に早番・遅番の氏名、スコア、未出勤者一覧を `<table>` で表示する部分を追加する（T-2 決定：表形式）
    - `@DisplayName` の先頭に `[F-4]` を付ける
  - 対象ファイル：
    - `src/main/resources/templates/index.html`
    - `src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
    - `@DisplayName` の先頭に `[F-4]` が付いたテストが存在する
    - 早番・遅番の氏名、スコア、未出勤者の氏名がレスポンス本文に含まれることを検証している

- [ ] **T7. [F-5] 不成立時のメッセージを表示する**
  - 依頼事項：
    - `ShiftControllerTest` に、`assign` が `Optional.empty()` を返すようスタブし、`POST /shift` のレスポンス本文に「条件を満たす組み合わせが見つかりませんでした。」という事実のみのメッセージが含まれ、結果テーブル（早番・遅番の見出しなど）が含まれないことを検証するテストを先に書き、RED を確認する
    - `index.html` に、`unassignable` が真の場合に固定文言のメッセージを表示する部分を追加する（T-3 決定：原因分析はしない）
    - `@DisplayName` の先頭に `[F-5]` を付ける
  - 対象ファイル：
    - `src/main/resources/templates/index.html`
    - `src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
    - `@DisplayName` の先頭に `[F-5]` が付いたテストが存在する
    - 不成立時のメッセージが事実のみ（原因を含まない）であることをテストで確認している

- [ ] **T8. 全体テストと画面表示の確認**
  - 依頼事項：
    - `./mvnw test` を実行し、Spotless・Checkstyle を含めて全テストが成功することを確認する
    - 未整形箇所があれば `./mvnw spotless:apply` を実行してから再確認する
    - Javadoc 未記載の public/protected/package-private なクラス・メソッドがないか確認する（`.agents/rules/javadoc.md`）
    - `./mvnw spring-boot:run` でアプリを起動し、`curl` で `GET /` のレスポンス HTML に入力欄・「行を追加」・「シフトを作成」ボタンが含まれることを確認する（アプリ起動後は必ず停止する）
  - 対象ファイル：なし（確認のみ）
  - 完了条件：
    - `./mvnw test` が成功する（テスト失敗・Spotless 違反・Checkstyle 違反のいずれもない）
    - `GET /` の HTML に入力欄・操作ボタンが含まれることを確認済みである

## 実行ログ

<!-- implementer が試行結果（失敗理由・リトライ回数）を追記する欄。作成時は空のままにする -->
