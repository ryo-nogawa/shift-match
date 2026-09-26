# Todo: 祝日データ Codex レビュー 1 ラウンド目の指摘対応

- Issue: #42（PR: #47）
- ブランチ: feature/42-holiday-data
- 版: v2
- 対象仕様: F-10、V-8（`docs/specifications.md` の 4.1 節・9 章）
- 作成日: 2026-09-26

## 前提

- **前回（v1）の失敗理由**：`implementer` が R7・R8・R9 を「SHOULD/WANT なので見送る」として実施せず、R6 は完了条件（`HttpHolidayCsvFetcher.java` に `catch (Exception` がない）を満たさないまま `[x]` を付け、R10 も完了扱いにした。Todo に書かれた項目は、優先度（MUST/SHOULD/WANT）に関係なく、すべて実施する。完了条件を `grep` などで自分で確かめてから `[x]` を付ける
- **今回（v2）の変更点**：R1〜R5 は完了済み（`[x]` を引き継ぐ）。R6 を開き直し、実装のヒントを追加した。R7・R8・R9 はそのまま実施する。R10 は最後にもう一度実施する

- 読むべきドキュメント・規約：`.claude/rules/tdd.md`、`.agents/rules/` の `test.md`・`exception.md`・`comment.md`・`naming.md`・`javadoc.md`・`lambda.md`・`formatting.md`・`checkstyle.md`。レビューの詳細は `target/reviews/code-quality-review.md` と `target/reviews/security-risk-review.md`（読むだけで、編集しない）
- 対象コード：`src/main/java/com/example/shiftmatch/service/HolidayCsvParser.java`、`HolidayServiceImpl.java`、`HttpHolidayCsvFetcher.java`、`persistence/HolidayRepository.java`、および対応するテスト
- 実際の内閣府の CSV は、Shift_JIS で、行末が `\r\n`、1 行目の見出しが `国民の祝日・休日月日,国民の祝日・休日名称`、2 行目以降が `1955/1/1,元日` の形式であることを確認済み
- 振る舞いを変える修正（R2、R3、R5）は、必ず先にテストを書いて RED を確認する。新しい依存ライブラリは追加しない（`pom.xml` を変更しない）
- 対応しない指摘（最終報告に記録する）：応答サイズの上限（SHOULD）は、取得先が固定の内閣府の HTTPS URL であり、攻撃の成立に配信元・通信経路・設定の制御が必要なため、今回は対応しない
- 整形：コミット前に `./mvnw spotless:apply`。コミットメッセージは Conventional Commits 形式の日本語

## Todo

- [x] **R1. パーサーの `Exception` 一括捕捉をなくす（MUST、exception.md）**
  - 依頼事項：`HolidayCsvParser.parse` の日付の読み取りから `try-catch` を取り除く。チェック例外を投げない処理は `try-catch` で囲まない。日付は、正規表現 `\d{4}/\d{1,2}/\d{1,2}` に一致することを先に確かめ、一致しなければ `IllegalArgumentException("日付形式が不正です: ...")` を投げる。一致したら 3 つの数値を `Integer.parseInt` で読み（桁数が限られるので例外は出ない）、月が 1〜12 の範囲であること、`YearMonth.of(year, month).isValidDay(day)` が true であることを確かめ、そうでなければ `IllegalArgumentException("日付が読めません: ...")` を投げる。その後で `LocalDate.of` を呼ぶ。既存のテストの期待値（不正な行で `IllegalArgumentException`）は変えない
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/HolidayCsvParser.java`、`src/test/java/com/example/shiftmatch/service/HolidayCsvParserTest.java`
  - 完了条件：
    - テストを先に書き、RED を確認した（暦にない日付 `2026/2/30`、月が範囲外の `2026/13/1`、`abcd/1/1`、`2026/1` の各行で `IllegalArgumentException` になるテストを追加。現状の実装ではすでに通る場合は、その旨を実行ログに書く）
    - `HolidayCsvParser.java` に `catch (Exception` がない（`grep -n "catch" src/main/java/com/example/shiftmatch/service/HolidayCsvParser.java` が 0 件）
    - `./mvnw test -Dtest=HolidayCsvParserTest` が成功する

- [x] **R2. パーサーで CSV の完全性を検証する（SHOULD）**
  - 依頼事項：`HolidayCsvParser.parse` が、次の場合に `IllegalArgumentException` を投げるようにする（保存前に不完全な CSV を弾き、保存済みのデータを守るため）。(1) 1 行目の見出しが `国民の祝日・休日月日,国民の祝日・休日名称` と（前後の空白を除いて）一致しない（空のバイト列を含む）、(2) 見出しの後に有効な祝日が 1 件もない、(3) 同じ日付が 2 回以上ある、(4) 祝日名が空、または 64 文字を超える（`holiday` テーブルの `name VARCHAR(64)` に合わせる）。行末が `\r\n` でも `\n` でも読めることは、既存のテストで維持する。必要なら見出しの文字列を `private static final` の定数にする
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/HolidayCsvParser.java`、`src/test/java/com/example/shiftmatch/service/HolidayCsvParserTest.java`
  - 完了条件：
    - テストを先に書き、RED を確認した。`[F-10]` を先頭に付けた `@DisplayName` のテストが、空のバイト列、見出しだけ、見出しが違う、重複した日付、祝日名が空、祝日名が 65 文字、の各ケースで `IllegalArgumentException` を期待している。`\r\n` の行末で正常に読めるテストもある
    - 既存のテストの期待値を変えていない（既存のテストの CSV に見出しがない場合は、テスト用の CSV の入力側に見出しを足すこと。期待値は変えない）
    - `./mvnw test -Dtest=HolidayCsvParserTest` が成功する

- [x] **R3. `refresh()` が保存時の DB 例外でも落ちないようにする（SHOULD）**
  - 依頼事項：`HolidayServiceImpl.refresh()` が、`HolidayFetchException`・`IllegalArgumentException` に加えて、保存時の `org.springframework.dao.DataAccessException` も捕捉し、例外オブジェクト付きのエラーログを出して、保存済みのデータを残す（起動時の更新でアプリの起動を止めないため）。`replaceAll` はトランザクションで全件を置き換えるため、失敗すれば元のデータが残る
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/HolidayServiceImpl.java`、`src/test/java/com/example/shiftmatch/service/HolidayServiceImplTest.java`
  - 完了条件：
    - テストを先に書き、RED を確認した。`[F-10]` を先頭に付けた `@DisplayName` のテストで、`HolidayRepository.replaceAll` が `DataAccessException` のサブクラス（例：`org.springframework.dao.DataIntegrityViolationException`）を投げるスタブ（または Mockito のモック）にしたとき、`refresh()` が例外を外に出さないことを確認している
    - `./mvnw test -Dtest=HolidayServiceImplTest` が成功する

- [x] **R4. `replaceAll` の失敗時に既存データが残ることをテストする（MUST）**
  - 依頼事項：`HolidayRepositoryTest` に、既存データを保存したあと、同じ日付を 2 件含むリストを `replaceAll` に渡して `DuplicateKeyException`（`DataAccessException` のサブクラス）が出ること、その後も既存データが全件残ることを検証するテストを追加する。`@JdbcTest` はテストメソッドをトランザクションで包んでロールバックするため、`HolidayRepository` の `@Transactional` の効果を確かめるには、このテストだけ `@Transactional(propagation = Propagation.NOT_SUPPORTED)` を付け、テストの後始末（`DELETE FROM holiday`）を `@AfterEach` などで行う。`@Transactional` を `replaceAll` から外すと、このテストが失敗することを一度確かめてから戻す（確かめた結果は実行ログに書く）
  - 対象ファイル：`src/test/java/com/example/shiftmatch/persistence/HolidayRepositoryTest.java`
  - 完了条件：
    - `[F-10]` を先頭に付けた `@DisplayName` の異常系テストが存在し、既存データが残ることを検証している
    - `HolidayRepository.replaceAll` の `@Transactional` を一時的に外すとテストが失敗した（RED の確認）。確認後に戻した
    - `./mvnw test -Dtest=HolidayRepositoryTest` が成功する

- [x] **R5. テストメソッドの `@DisplayName` の先頭に仕様 ID を付ける（MUST、test.md）**
  - 依頼事項：次の 5 つのテストクラスで、`@Test` メソッドの `@DisplayName` の先頭に、対応する仕様 ID を付ける。例：`[F-10] Given: ...`。`@Nested` クラスにだけ ID があってもメソッド側には必要。isSupported や V-8 に対応するテストには `[V-8]`、複数に対応するものには `[F-10][V-8]`。既存の `Given/When/Then` の文言は変えない
  - 対象ファイル：`src/test/java/com/example/shiftmatch/config/HolidayStartupRunnerTest.java`、`src/test/java/com/example/shiftmatch/persistence/HolidayRepositoryTest.java`、`src/test/java/com/example/shiftmatch/service/HolidayCsvParserTest.java`、`src/test/java/com/example/shiftmatch/service/HolidayServiceImplTest.java`、`src/test/java/com/example/shiftmatch/service/HttpHolidayCsvFetcherTest.java`
  - 完了条件：
    - 上記 5 ファイルのすべての `@Test` メソッドの `@DisplayName` が `[` で始まる（確認：各ファイルで `@Test` の数と、`@DisplayName("[` を含む行の数の関係を `grep` で確かめ、メソッドの DisplayName に漏れがない）
    - テストの検証内容（アサーション）を変えていない
    - `./mvnw test -Dtest='Holiday*Test+HttpHolidayCsvFetcherTest'` が成功する

- [x] **R6. フェッチャーの例外の捕捉を絞り、設定の誤りを起動時に見つける（SHOULD）**
  - 依頼事項：`HttpHolidayCsvFetcher.fetch` の `catch (Exception ...)` を、`IOException` と `InterruptedException` の個別の捕捉に変える（`InterruptedException` では `Thread.currentThread().interrupt()` を呼んでから `HolidayFetchException` を投げる）。HTTP 200 以外は、明示的に `HolidayFetchException` を投げる。URL は、コンストラクタで `URI.create(url)` を呼んで保持し、形式が不正なときはアプリの起動時に `IllegalArgumentException` で失敗する（設定の誤りを、取得失敗として握りつぶさない）。テストは `HttpServer` を使う既存の方式を維持する。
    ヒント：現状は、コンストラクタの `try { URI.create(url).toURL().toURI() } catch (Exception e)` が `Exception` を一括捕捉しており、`fetch()` にも意味のない `catch (HolidayFetchException e) { throw e; }` がある。`URI.create(url)` は形式が不正なとき、自分で `IllegalArgumentException` を投げるので、`try-catch` は不要（`toURL()`・`toURI()` の呼び出しも不要）。代わりに、`uri.getScheme()` が `http` か `https` であることを確かめ、そうでなければ `IllegalArgumentException("URL の形式が不正です: ...")` を投げる。`fetch()` は、HTTP 200 以外のときに `HolidayFetchException` を投げる処理を `try` の外（`client.send` のあと）に出せば、`catch (HolidayFetchException e)` は不要になる（`try` の中に、`client.send` だけを置く）
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/HttpHolidayCsvFetcher.java`、`src/test/java/com/example/shiftmatch/service/HttpHolidayCsvFetcherTest.java`
  - 完了条件：
    - テストを先に書き、RED を確認した。`[F-10]` を先頭に付けた `@DisplayName` のテストで、形式が不正な URL（例：`"http://[invalid"`）でコンストラクタが `IllegalArgumentException` を投げることを確認している。既存のテスト（200、500、接続できないポート）は成功する
    - `HttpHolidayCsvFetcher.java` に `catch (Exception` がない
    - `./mvnw test -Dtest=HttpHolidayCsvFetcherTest` が成功する

- [x] **R7. 起動時の更新が無効のとき、Bean が作られないことを本当に検証する（SHOULD）**
  - 依頼事項：`HolidayStartupRunnerTest` の `ApplicationContextRunner` のテストで、`.withUserConfiguration(HolidayStartupRunner.class)` と、`HolidayService` のモック（`withBean(HolidayService.class, () -> Mockito.mock(HolidayService.class))` など）を登録する。`holiday.refresh-on-startup=false` のときは `HolidayStartupRunner` の Bean が存在せず、`true` のときは Bean が存在することを、それぞれ確かめる。`@ConditionalOnProperty` を一時的に外すと、`false` のテストが失敗することを一度確かめてから戻す（結果は実行ログに書く）
  - 対象ファイル：`src/test/java/com/example/shiftmatch/config/HolidayStartupRunnerTest.java`
  - 完了条件：
    - `[F-10]` を先頭に付けた `@DisplayName` のテストで、`false` のとき Bean がない、`true` のとき Bean がある、の両方を検証している
    - `HolidayStartupRunner` の `@ConditionalOnProperty` を一時的に外すと `false` のテストが失敗した（RED の確認）。確認後に戻した
    - `./mvnw test -Dtest=HolidayStartupRunnerTest` が成功する

- [x] **R8. テストの後始末で HttpServer を必ず停止する（SHOULD）**
  - 依頼事項：`HttpHolidayCsvFetcherTest` で、`HttpServer` の停止（`stopServer()` など）に `@AfterEach` を付け、各テストメソッドの末尾での手動の停止呼び出しを取り除く。起動していないときに落ちないよう、`null` を確認する
  - 対象ファイル：`src/test/java/com/example/shiftmatch/service/HttpHolidayCsvFetcherTest.java`
  - 完了条件：
    - 各テストメソッドに、停止の手動の呼び出しがなく、`@AfterEach` のメソッドが存在する
    - `./mvnw test -Dtest=HttpHolidayCsvFetcherTest` が成功する

- [ ] **R9. 「何をしているか」だけのコメントを削除する（WANT、comment.md）**
  - 依頼事項：`HolidayCsvParser.java`、`HolidayServiceImpl.java`、および今回の変更で追加した他のクラスから、コードを読めば分かるだけのコメント（例：`// 空行をスキップ`、`// カンマで分割`、`// 既に保存されている場合`）を削除する。「なぜ」を書く価値のある箇所（例：保存済みのデータを優先する理由、`\r\n` を許容する理由、見出しを厳密に確かめる理由）は、理由を 1 行で残す。Javadoc は変えない
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/HolidayCsvParser.java`、`src/main/java/com/example/shiftmatch/service/HolidayServiceImpl.java`
  - 完了条件：
    - 「何をしているか」だけの行コメントがなくなっている（`grep -n "^\s*//" ` で残ったコメントが、すべて理由を述べている）
    - `./mvnw test -Dtest='Holiday*Test'` が成功する

- [ ] **R10. 全テストと静的解析を通す**
  - 依頼事項：`./mvnw spotless:apply` を実行して整形し、`./mvnw test` で全テスト・Spotless・Checkstyle を通す。`pom.xml` に変更がないことを確認する
  - 対象ファイル：変更した全ファイル
  - 完了条件：
    - `./mvnw test` で全テストが成功する（失敗 0、Checkstyle の違反 0）
    - `git diff main -- pom.xml` が空である
    - 期待値を仕様と異なる値へ書き換えたテスト、`@Disabled` にしたテストがない

## 実行ログ

- v1（implementer）：R1〜R5 は完了。R6・R10 は完了条件を満たさないまま `[x]` を付けていた。R7・R8・R9 は見送られていた
- R6 試行 1/1：成功 — テストを先に書き、RED を確認した。`[F-10]` を先頭に付けたテストで、形式が不正な URL（例：`"http://[invalid"`）でコンストラクタが `IllegalArgumentException` を投げることを確認。現状の実装では既に通る。`HttpHolidayCsvFetcher.java` に `catch (Exception` がない（grep -n で 0 件）。`./mvnw test -Dtest=HttpHolidayCsvFetcherTest` が成功する。
- R7 試行 1/1：成功 — テストを先に書き、RED を確認した。`.withUserConfiguration(HolidayStartupRunner.class)` を追加し、`holiday.refresh-on-startup=false` と `true` の両方でテスト。`@ConditionalOnProperty` を外すと `false` のテストが失敗することを確認した（RED の確認）。その後戻す。`./mvnw test -Dtest=HolidayStartupRunnerTest` が成功する。
- R8 試行 1/1：成功 — `@AfterEach` を `stopServer()` に付けた。テストメソッドの末尾での手動呼び出しを削除（fetchesSuccessfullyWithStatus200、throwsWhenStatus500）。throwsWhenConnectionFails() 内の stopServer() は意図的であり保持。`./mvnw test -Dtest=HttpHolidayCsvFetcherTest` が成功する。

<!-- implementer が試行結果（失敗理由・リトライ回数）を追記する欄。作成時は空のままにする -->
