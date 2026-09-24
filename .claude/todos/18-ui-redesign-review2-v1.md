# Todo: Codex レビュー 2 ラウンド目の指摘対応（画面デザイン刷新）

- Issue: #18
- ブランチ: feature/18-ui-redesign
- 版: v1
- 対象仕様: F-4（テストの品質改善のみ。振る舞いの変更なし）
- 作成日: 2026-09-24

## 前提

- 読むべきドキュメント・規約：`target/reviews/code-quality-review.md`（指摘の全文）、`.agents/rules/comment.md`、`.agents/rules/test.md`、`.claude/rules/tdd.md`
- 変更してよいのは次のファイルのみ：`src/main/resources/static/js/shift-form.js`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`。`index.html`・CSS・Java 本体・`pom.xml`・`docs/` は変更しない。**この Todo はすべてリファクタリング・テスト改善で、画面の振る舞いは変えない**
- 1 Todo が終わるたびに、チェックと実行ログを更新して Conventional Commits 形式（日本語）でコミットしてから次へ進む。コミットメッセージの末尾に `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>` を付ける
- 各 Todo の前後で `./mvnw test -Dtest=ShiftControllerTest` の件数が変わらないこと（テストの追加・削除をしない）

## Todo

- [x] **T1. 「何をしているか」を言い換えるだけのコメントを削除する（MUST 指摘）**
  - 依頼事項：`.agents/rules/comment.md` は、コメントに「何をしているか」ではなく「なぜこの実装にしたか」を書くことを求めている。今回の変更で追加された、直後のコードを言い換えるだけのコメントを削除する。対象は次のとおり（行番号は目安。`git diff main -- src/main/resources/static/js/shift-form.js src/test` の追加行で `//` や `/*` を含む行を探して確認する）
    - `shift-form.js`：`// 初期化：既存の select の data-value を現在の value に設定`、`// select の変更時に data-value を更新`、`// ヘルパーメソッド`（`createWishSelect` の直前など）
    - `ShiftControllerTest.java`：`// class="empty" 要素の範囲を特定`、`// タイムラインの範囲を切り出し、tl-name から氏名を抽出`、および同様に「何を」だけを説明しているコメント（`UiDesignTest` 内のヘルパーメソッド直前のコメントを含む）
    - 例外：「なぜ」が書かれているコメントは残してよい。`shift-form.js` の既存コメント `// インデックスに欠番があると Spring MVC でリストをバインドできないため、削除後に振り直す` は残す。`tl-name` の抽出で範囲を切り出している箇所には、理由のコメントを 1 行だけ残す（例：`// 結果表にも同じ氏名が出るため、タイムラインの範囲に限定して抽出する`）
  - 対象ファイル：`src/main/resources/static/js/shift-form.js`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `grep -n "ヘルパーメソッド\|要素の範囲を特定\|氏名を抽出\|data-value を更新\|現在の value に設定" src/main/resources/static/js/shift-form.js src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java` が 0 件である
    - `git diff main` の追加行のコメントが、すべて「なぜ」を説明している（実行ログに、残したコメントの一覧と理由を記録する）
    - `node --check src/main/resources/static/js/shift-form.js` が成功する
    - `./mvnw test -Dtest=ShiftControllerTest` が成功し、件数が変わらない

- [ ] **T2. タイムライン関連テストの重複したパラメータ構築を `createValidParams()` に置き換える（リファクタリング）**
  - 依頼事項：`ShiftControllerTest` の `UiDesignTest` 内で、`createValidParamsSingleEmployee()` の後に残り 3 名分の同一パラメータを毎回追加しているテスト（タイムライン関連の 6 件。`grep -n "createValidParamsSingleEmployee" src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java` で探す）を、4 名分を返す既存の `createValidParams()` の呼び出しに置き換える。置き換え前に `createValidParams()` の中身が、各テストで追加している 4 名分の内容と同じであることを確認する（違いがある場合は置き換えず、実行ログに理由を書く）。置き換え後、`createValidParamsSingleEmployee()` が使われなくなれば削除する。各テストの `@DisplayName`・期待値は変えない
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `./mvnw test -Dtest=ShiftControllerTest` の件数が変わらず、すべて成功する（前後の件数を実行ログに記録する）
    - タイムライン関連テストに、3 名分の重複したパラメータ追加コードが残っていない（`grep -n "employees\[3\]" src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java` の出現数が、ヘルパー内の定義と他の用途を除いて減っている。前後の数値を記録する）

- [ ] **T3. UI テストの検証を、対象要素の範囲に限定する（テスト改善）**
  - 依頼事項：`UiDesignTest` の次のテストは、レスポンス本文全体に対する `contains` で判定しており、対象要素の外に値が移動しても成功してしまう。対象要素の範囲を切り出してから検証するようにする。①スコア（`class="score-num"` の要素の中にスコア `3` と ` / 4` がある）、②未出勤者（`class="unassigned"` の要素の中に `class="chip"` と `五郎` がある）、③結果表（`class="result-table"` の `<table>` の中に `pill early` が 2 つ、`pill late` が 2 つある）、④タイムラインの勤務バー・休憩バー・目盛り・凡例（`class="timeline"` の範囲の中で判定する）。範囲を切り出すヘルパー `extractSection(String body, String startMarker, String endMarker)`（開始マーカーの位置から、その後ろの終了マーカーの位置までの `substring` を返す。見つからなければ `fail`）を `UiDesignTest` に 1 つ追加して使う。要素の終了位置が特定しづらい場合は、次のセクションの開始マーカー（例：`class="result-table"`）までを範囲としてよい。出現数を数える検証は `Matcher` を使う。各テストの `@DisplayName` と検証内容（期待値）は変えない。**検出力の確認**：`index.html` で `score-num` の中のスコアを一時的に別の場所へ移す（または `class="unassigned"` の外へ `五郎` を移す）と、対応するテストが失敗することを確認し、確認後に元へ戻す（コミットには含めない）
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `./mvnw test -Dtest=ShiftControllerTest` の件数が変わらず、すべて成功する（前後の件数を実行ログに記録する）
    - スコア・未出勤者の 2 テストで、一時的に要素の外へ値を移すと失敗することを確認した（実行ログに記録する）
    - `@DisplayName` の文言が変更されていない（`git diff` で `@DisplayName` の行に差分がない）

- [ ] **T4. 全テストと静的解析が成功することを確認する**
  - 依頼事項：`./mvnw spotless:apply` を実行してから `./mvnw test` を実行する。失敗があれば原因を修正する（テストの期待値を仕様と異なる値へ書き換えない）。`git status` で未コミットの差分がないことを確認する
  - 対象ファイル：（変更なし。確認のみ）
  - 完了条件：
    - `./mvnw test` が成功する（テスト件数・Checkstyle 違反 0 件・Spotless 違反なしを実行ログに記録する）
    - `git diff --check main` が成功する
    - Java 本体（`src/main/java/`）・`pom.xml`・`index.html`・CSS に、この Todo による差分がない

## 実行ログ

- T1 完了：削除したコメント「現在の value に設定」「data-value を更新」「ヘルパーメソッド」「要素の範囲を特定」を grep で確認（0件）。残したコメント「結果表にも同じ氏名が出るため、タイムラインの範囲に限定して抽出する」は「なぜ」を説明。node --check 成功、./mvnw test -Dtest=ShiftControllerTest 成功（41件）
