# Todo: 祝日データの取得と保存（月間 1/5）

- Issue: #42（親 Issue: #40）
- ブランチ: feature/42-holiday-data
- 版: v1
- 対象仕様: F-10、V-8（`docs/specifications.md` の 4.1 節・4.2 節・9 章）
- 作成日: 2026-09-26

## 前提

- 読むべきドキュメント・規約：`docs/specifications.md` の 4.1 節（祝日の段落）・4.2 節（V-8）・9 章、`.claude/rules/tdd.md`、`.agents/rules/` の `test.md`・`naming.md`・`javadoc.md`・`comment.md`・`exception.md`・`config.md`・`lambda.md`・`thread-safety.md`・`formatting.md`・`checkstyle.md`
- 仕様の要点
  - 祝日の正規データは内閣府の「国民の祝日」CSV（`https://www8.cao.go.jp/chosei/shukujitsu/syukujitsu.csv`、文字コード Shift_JIS）。1 行目は見出し（`国民の祝日・休日月日,国民の祝日・休日名称`）、2 行目以降は `1955/1/1,元日` の形式（日付は `yyyy/M/d`）
  - 取得は JDK 標準の `java.net.http.HttpClient` を使い、H2 に保存する。**新しい依存ライブラリは追加しない**（`pom.xml` を変更しない）
  - 取得はアプリの起動時と、対象年の祝日が保存済みデータにないとき。取得に失敗したら、保存済みのデータを使う（例外で落とさない）
  - 保存済みデータでも判定できない年月は V-8 のエラーにするための判定を提供する。「年が判定できる」とは、その年の祝日が 1 件以上保存されていることとする（どの年にも元日がある）
  - 営業日は「月〜金かつ祝日でない日」
- 既存コードとの関係
  - 保存は `persistence/LatestShiftRepository.java` と同じく `JdbcClient` を使う。テーブルは `src/main/resources/schema.sql` に追加する（`CREATE TABLE IF NOT EXISTS`）
  - サービスは `service/ShiftAssignmentService`（インタフェース）と `ShiftAssignmentServiceImpl`（実装）の命名に倣う
  - 例外は `domain/` の既存の `*Error`（例：`DuplicateNameError`）の作り方に倣う
  - 環境依存値（CSV の URL、タイムアウト秒数）は `application.properties` に定義し `@Value` で注入する（`config.md`）。コードにリテラルで書かない
  - メソッド参照（`Type::method`）は使わない（`lambda.md`。Checkstyle の `BanMethodReference` で検出される）
  - 例外を捕捉したら SLF4J のロガーに例外オブジェクトを渡してスタックトレースを出す（`exception.md`）
- テストの注意
  - **テストはネットワーク（外部の内閣府サイト）に依存させない。** CSV の取得は、JDK の `com.sun.net.httpserver.HttpServer` をテスト内で localhost に起動して検証する。CSV の内容はテスト内の文字列を `Shift_JIS` でバイト列にして使う
  - 仕様に対応するテストの `@DisplayName` の先頭に仕様 ID（`[F-10]`、`[V-8]`）を付ける
  - Spring のコンテキスト全体を起動するテスト（`@SpringBootTest`）が起動時の取得で外部にアクセスしないよう、T7 で対処する
- 整形：コミット前に `./mvnw spotless:apply` を実行する。コミットメッセージは Conventional Commits 形式の日本語（例：`feat: [F-10] 祝日 CSV のパーサーを追加する`）

## Todo

- [x] **T1. 祝日 CSV を解析するパーサーを作る（F-10）**
  - 依頼事項：`domain/Holiday.java`（`record Holiday(LocalDate date, String name)`）と、`service/HolidayCsvParser.java`（`List<Holiday> parse(byte[] csv)`）を作る。`csv` は Shift_JIS のバイト列。1 行目の見出しは読み飛ばし、空行は無視し、日付は `yyyy/M/d`（例：`2026/1/1`、`2026/10/12`）として読む。形式が不正な行（日付が読めない、列が足りない）があれば `IllegalArgumentException` を投げる（一部だけ読み込んで保存しないため）。文字コードは `Charset.forName("Shift_JIS")`
  - 対象ファイル：`src/main/java/com/example/shiftmatch/domain/Holiday.java`、`src/main/java/com/example/shiftmatch/service/HolidayCsvParser.java`、`src/test/java/com/example/shiftmatch/service/HolidayCsvParserTest.java`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `[F-10]` を含む `@DisplayName` のテストが存在し、Given-When-Then で書かれている：見出し行の読み飛ばし、日本語の祝日名（例：`スポーツの日`）が文字化けしない、空行の無視、`2026/10/12` が `LocalDate.of(2026, 10, 12)` になる、不正な行で `IllegalArgumentException`
    - `./mvnw test -Dtest=HolidayCsvParserTest` が成功する

- [x] **T2. 祝日テーブルと HolidayRepository を作る（F-10）**
  - 依頼事項：`schema.sql` に `holiday` テーブル（`holiday_date DATE PRIMARY KEY`、`name VARCHAR(64) NOT NULL`）を追加する。`persistence/HolidayRepository.java`（`@Repository`、`JdbcClient` を使う）に次を実装する：`void replaceAll(List<Holiday> holidays)`（`@Transactional`。全件削除してから登録するため、途中で失敗しても元のデータが残る）、`List<Holiday> findByYear(int year)`（日付順）、`boolean existsInYear(int year)`。既存の `LatestShiftRepositoryTest.java` と `SchemaTest.java` のテストの作り方に倣う
  - 対象ファイル：`src/main/resources/schema.sql`、`src/main/java/com/example/shiftmatch/persistence/HolidayRepository.java`、`src/test/java/com/example/shiftmatch/persistence/HolidayRepositoryTest.java`、`src/test/java/com/example/shiftmatch/persistence/SchemaTest.java`（テーブルの存在確認を追加）
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `[F-10]` を含む `@DisplayName` のテストが存在する：保存して `findByYear` で年ごとに取得できる（日付順）、`replaceAll` が既存の全件を置き換える、`existsInYear` が保存済みの年で true・ない年で false、`SchemaTest` で `holiday` テーブルが作られる
    - `./mvnw test -Dtest=HolidayRepositoryTest+SchemaTest` が成功する

- [x] **T3. 祝日 CSV を取得するクラスを作る（F-10）**
  - 依頼事項：`service/HolidayCsvFetcher.java`（インタフェース：`byte[] fetch()`）と、`service/HttpHolidayCsvFetcher.java`（実装、`@Component`）を作る。実装は `java.net.http.HttpClient` で GET し、HTTP 200 のときだけボディをバイト列で返す。CSV の URL と、接続・応答のタイムアウト秒数は `application.properties` に `holiday.csv.url`（値は `https://www8.cao.go.jp/chosei/shukujitsu/syukujitsu.csv`）と `holiday.csv.timeout-seconds`（値は `10`）で定義し、`@Value` で注入する。200 以外のステータス、接続失敗、タイムアウトのときは、`service/HolidayFetchException.java`（`RuntimeException`。原因の例外を保持する）を投げる。`InterruptedException` を捕捉したときは、`Thread.currentThread().interrupt()` を呼んでから `HolidayFetchException` を投げる
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/HolidayCsvFetcher.java`、`src/main/java/com/example/shiftmatch/service/HttpHolidayCsvFetcher.java`、`src/main/java/com/example/shiftmatch/service/HolidayFetchException.java`、`src/main/resources/application.properties`、`src/test/java/com/example/shiftmatch/service/HttpHolidayCsvFetcherTest.java`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - テストは `com.sun.net.httpserver.HttpServer`（localhost、ポートは 0 で空きを自動割り当て）を使い、外部ネットワークにアクセスしない。`[F-10]` を含む `@DisplayName` のテストが存在する：200 のとき Shift_JIS のバイト列がそのまま返る、500 のとき `HolidayFetchException`、接続できないポート（起動して閉じたサーバーのポート）のとき `HolidayFetchException`
    - URL とタイムアウトのリテラルが Java コードにない（`grep -rn "cao.go.jp" src/main/java` が 0 件）
    - `./mvnw test -Dtest=HttpHolidayCsvFetcherTest` が成功する

- [x] **T4. 祝日データを更新するサービスを作る：失敗時は保存済みを使う（F-10）**
  - 依頼事項：`service/HolidayService.java`（インタフェース）と `service/HolidayServiceImpl.java`（`@Service`）を作り、まず `void refresh()` を実装する。`HolidayCsvFetcher` で取得 → `HolidayCsvParser` で解析 → `HolidayRepository.replaceAll` で保存する。取得（`HolidayFetchException`）または解析（`IllegalArgumentException`）に失敗したときは、例外を外に投げず、SLF4J のロガーで例外オブジェクト付きのエラーログを出し、保存済みのデータを変更しない。同時に複数のスレッドが `refresh` を呼んでも、保存が競合しないよう `synchronized` にする（`thread-safety.md`）。テストでは `HolidayCsvFetcher` を Mockito のモックまたはテスト用のスタブに差し替え、`HolidayRepository` は H2 のテスト（T2 の作り方）または Mockito で検証する
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/HolidayService.java`、`src/main/java/com/example/shiftmatch/service/HolidayServiceImpl.java`、`src/test/java/com/example/shiftmatch/service/HolidayServiceImplTest.java`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `[F-10]` を含む `@DisplayName` のテストが存在する：取得に成功すると解析した祝日が保存される、取得に失敗しても例外が外に出ず保存済みのデータが残る、CSV が不正で解析に失敗しても保存済みのデータが残る
    - `./mvnw test -Dtest=HolidayServiceImplTest` が成功する

- [ ] **T5. 対象月が判定できるかを返す：V-8 の判定（V-8、F-10）**
  - 依頼事項：`HolidayService` に `boolean isSupported(YearMonth month)` を追加して実装する。対象年の祝日が保存済み（`existsInYear`）なら、取得せずに true を返す。保存されていなければ `refresh()` を 1 回呼び、その後もう一度 `existsInYear` で判定して返す（取得失敗で保存済みのデータにもなければ false）
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/HolidayService.java`、`src/main/java/com/example/shiftmatch/service/HolidayServiceImpl.java`、`src/test/java/com/example/shiftmatch/service/HolidayServiceImplTest.java`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `[V-8]` を含む `@DisplayName` のテストが存在する：保存済みの年は true で取得を呼ばない、保存されていない年は取得して true になる、保存されておらず取得にも失敗した年は false
    - `./mvnw test -Dtest=HolidayServiceImplTest` が成功する

- [ ] **T6. 営業日と祝日の一覧を返す（F-10、V-8）**
  - 依頼事項：`HolidayService` に `List<LocalDate> businessDays(YearMonth month)`（月〜金かつ祝日でない日を日付順に返す）と `Map<LocalDate, String> holidaysOf(YearMonth month)`（その月の祝日の日付と祝日名。日付順の `LinkedHashMap` または `TreeMap`）を追加して実装する。どちらも、最初に `isSupported(month)` を確認し、false のときは `domain/HolidayDataUnavailableError.java`（`RuntimeException`。`domain/` の既存の `*Error` に倣う。メッセージに年月を含める）を投げる（V-8）。祝日が土日と重なる日は、営業日に含まれないだけで、`holidaysOf` には含める。2026 年 10 月（12 日がスポーツの日）を検証に使う：営業日は 10/1〜10/30 の月〜金から 10/12 を除いた 21 日
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/HolidayService.java`、`src/main/java/com/example/shiftmatch/service/HolidayServiceImpl.java`、`src/main/java/com/example/shiftmatch/domain/HolidayDataUnavailableError.java`、`src/test/java/com/example/shiftmatch/service/HolidayServiceImplTest.java`
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `[F-10]` を含む `@DisplayName` のテスト：2026 年 10 月の営業日が 21 日で 10/12 を含まず、土日を含まない。`[V-8]` を含むテスト：判定できない年月で `HolidayDataUnavailableError`。`holidaysOf` が祝日名付きで返る、土曜と重なる祝日（テスト用 CSV に `2026/10/10`（土曜）を加えて、営業日の件数が変わらず、`holidaysOf` には含まれること）
    - `./mvnw test -Dtest=HolidayServiceImplTest` が成功する

- [ ] **T7. 起動時に祝日データを更新する（F-10）**
  - 依頼事項：`config/HolidayStartupRunner.java`（`@Component`、`ApplicationRunner`）を作り、`HolidayService.refresh()` を呼ぶ。`@ConditionalOnProperty(name = "holiday.refresh-on-startup", havingValue = "true")` で有効・無効を切り替え、`application.properties` に `holiday.refresh-on-startup=true` を定義する。取得に失敗してもアプリの起動を止めない（`refresh` が例外を出さないため、追加の処理は不要）。`@SpringBootTest` を使う既存のテスト（`grep -rn "@SpringBootTest" src/test` で探す。例：`ShiftMatchApplicationTests.java`）は、外部にアクセスしないよう `properties = "holiday.refresh-on-startup=false"` を指定する。`ShiftControllerTest` など `@WebMvcTest` を使うテストで、新しい Bean が原因で失敗する場合は、`@MockitoBean` で `HolidayService` を差し替える
  - 対象ファイル：`src/main/java/com/example/shiftmatch/config/HolidayStartupRunner.java`、`src/main/resources/application.properties`、`src/test/java/com/example/shiftmatch/config/HolidayStartupRunnerTest.java`、既存の `@SpringBootTest` を使うテスト
  - 完了条件：
    - テストを先に書き、アサーションで失敗（RED）することを確認した
    - `[F-10]` を含む `@DisplayName` のテストが存在する：`run` を呼ぶと `HolidayService.refresh()` がちょうど 1 回呼ばれる（Mockito）、`refresh-on-startup=false` のときは Bean が作られない（`ApplicationContextRunner` を使う）
    - `grep -rn "@SpringBootTest" src/test` の各テストが `holiday.refresh-on-startup=false` を指定している
    - `./mvnw test` の実行中に、内閣府のサイトへの接続を試みていない（ログに `www8.cao.go.jp` へのアクセスが出ない）

- [ ] **T8. 全テストと静的解析を通す**
  - 依頼事項：`./mvnw spotless:apply` を実行して整形し、`./mvnw test` で全テスト・Spotless・Checkstyle を通す。違反があれば直す（振る舞いを変える修正は、テストを先に書く）。`pom.xml` に変更がないことを確認する
  - 対象ファイル：変更した全ファイル
  - 完了条件：
    - `./mvnw test` で全テストが成功する（失敗 0、Checkstyle の違反 0）
    - `git diff main -- pom.xml` が空である（新しい依存を追加していない）
    - 期待値を仕様と異なる値へ書き換えたテスト、`@Disabled` にしたテストがない

## 実行ログ

<!-- implementer が試行結果（失敗理由・リトライ回数）を追記する欄。作成時は空のままにする -->
