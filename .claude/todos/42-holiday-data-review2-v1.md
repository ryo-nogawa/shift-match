# Todo: 祝日データ Codex レビュー 2 ラウンド目の指摘対応

- Issue: #42（PR: #47）
- ブランチ: feature/42-holiday-data
- 版: v1
- 対象仕様: F-10、V-8
- 作成日: 2026-09-26

## 前提

- 読むべきドキュメント・規約：`.claude/rules/tdd.md`、`.agents/rules/test.md`・`comment.md`・`formatting.md`・`checkstyle.md`。レビューの詳細は `target/reviews/code-quality-review.md`（読むだけで、編集しない）
- **Todo に書かれた項目は、優先度に関係なくすべて実施する（見送り禁止）。完了条件はコマンドで自分で確かめてから `[x]` を付ける**
- どちらも振る舞いを変えない修正（表示名の修正と、定数の参照）。テストの期待値や検証内容は変えない。新しい依存ライブラリは追加しない（`pom.xml` を変更しない）
- 対応しない指摘（最終報告に記録する）：セキュリティーの SHOULD 2 件（応答サイズの上限、CSV の収録年の連続性の検証）は、取得先が固定の内閣府の HTTPS URL で、攻撃の成立に配信元・通信経路・設定の制御が必要なため、今回は対応しない
- 整形：コミット前に `./mvnw spotless:apply`。コミットメッセージは Conventional Commits 形式の日本語

## Todo

- [ ] **S1. `isSupported` のテストの `@DisplayName` を `[V-8]` にする（MUST、test.md）**
  - 依頼事項：`HolidayServiceImplTest.java` の、外側が `[V-8]` の `@Nested` クラス（`IsSupported`）にある 3 つのテストメソッド（111、123、137 行目付近）の `@DisplayName` の先頭を `[F-10]` から `[V-8]` に直す（取得・保存の検証も意図しているなら `[F-10][V-8]`）。`Given/When/Then` の文言とアサーションは変えない。ほかのテストメソッドも、対応する仕様と `@DisplayName` の先頭の ID が食い違っていないか、あわせて確かめる（`isSupported`・営業日の判定に関するテストは `[V-8]`）
  - 対象ファイル：`src/test/java/com/example/shiftmatch/service/HolidayServiceImplTest.java`
  - 完了条件：
    - `IsSupported` の 3 つのテストメソッドの `@DisplayName` が `[V-8]` で始まる（`grep -n "DisplayName" src/test/java/com/example/shiftmatch/service/HolidayServiceImplTest.java` で確認）
    - `./mvnw test -Dtest=HolidayServiceImplTest` が成功する

- [ ] **S2. 未使用の見出し定数 `EXPECTED_HEADER` を使う（SHOULD）**
  - 依頼事項：`HolidayCsvParser.java` の `EXPECTED_HEADER` は宣言されているが使われておらず、見出しの検証（52〜53 行目付近）に同じ文字列がリテラルで書かれている。見出しの文字列の定義元を 1 つにするため、検証の比較で `EXPECTED_HEADER` を使い、重複したリテラルを取り除く（見出しの列ごとに別々の定数があるなら、どれか 1 つの方式に統一し、未使用の定数を残さない）。振る舞いは変えない
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/HolidayCsvParser.java`
  - 完了条件：
    - `grep -n "国民の祝日・休日月日" src/main/java/com/example/shiftmatch/service/HolidayCsvParser.java` の結果が、定数の定義の行だけ（または列名ごとの定数の定義の行だけ）になっている
    - `./mvnw test -Dtest=HolidayCsvParserTest` が成功する

- [ ] **S3. 全テストと静的解析を通す**
  - 依頼事項：`./mvnw spotless:apply` を実行して整形し、`./mvnw test` で全テスト・Spotless・Checkstyle を通す。`pom.xml` に変更がないことを確認する
  - 対象ファイル：変更した全ファイル
  - 完了条件：
    - `./mvnw test` で全テストが成功する（失敗 0、Checkstyle の違反 0）
    - `git diff main -- pom.xml` が空である
    - `@Disabled` にしたテストがない

## 実行ログ

<!-- implementer が試行結果（失敗理由・リトライ回数）を追記する欄。作成時は空のままにする -->
