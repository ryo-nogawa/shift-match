# Todo: #32 Codex レビュー 2 ラウンド目の指摘修正

- Issue: #32
- ブランチ: feature/32-persist-latest-shift
- 版: v1
- 対象仕様: Issue #32 の記載。既存の H-1〜H-3、V-1〜V-5 の挙動は変えない
- 作成日: 2026-09-26

## 前提

- 読むべきもの：`target/reviews/code-quality-review.md`、`.claude/rules/tdd.md`、`.agents/rules/test.md`、`.agents/rules/comment.md`
- **新しい依存は追加しない。`pom.xml`・`docs/`・`AGENTS.md` は変更しない**
- テスト規約：`@DisplayName` は日本語の `Given: ...、When: ...、Then: ...` 形式。`@Nested` 側の表示名は変えない

## Todo

- [x] **T1. `Origin: null` などホストを持たない Origin を 403 にする**
  - 依頼事項：`src/main/java/com/example/shiftmatch/config/SameOriginInterceptor.java` で、`Origin` から取り出したホストが `null`（`Origin: null`、ホストを抽出できない値）のとき、`NullPointerException` にせず拒否（403）する。構文が不正で `URI` を作れない値も 403 のままであること。テストを先に書く：`Origin: null` と `Host: localhost:8080` で `POST /shift` → 403（RED：今は 500）、`Origin: file:///x` のようにホストのない構文上有効な値 → 403。いずれも `save`・`assign` が呼ばれない
  - 対象ファイル：`SameOriginInterceptor.java`、T4 で追加した同インターセプターのテストがあるファイル（`ShiftControllerTest.java` または `SameOriginInterceptorTest.java`）
  - 完了条件：
    - テストを先に書き、500（または例外）で RED になることを確認した
    - 既存の同一オリジン許可・別オリジン拒否のテストが成功する（`./mvnw test -Dtest=ShiftControllerTest`）
- [x] **T2. `ShiftControllerTest` の #32 追加分の `@DisplayName` を Given-When-Then にする**
  - 依頼事項：`ShiftControllerTest.java` の #32 で追加した 8 テストメソッド（保存・復元・保存失敗・氏名の最大長・ログなど。`git diff main -- src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java` で追加分を確認する）の `@DisplayName` を `Given: ...、When: ...、Then: ...` の形にする。T1 で追加するテストも同じ形にする。振る舞いは変えない
  - 対象ファイル：`ShiftControllerTest.java`（必要なら `SameOriginInterceptorTest.java`）
  - 完了条件：
    - #32 で追加したすべてのテストメソッドの `@DisplayName` が `Given:` で始まり `When:`・`Then:` を含む（`git diff main -U0 -- <ファイル> | grep -A1 "DisplayName"` で確認）
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
- [ ] **T3. 復元テストで 12 行すべての開始・終了を検証する**
  - 依頼事項：`ShiftControllerTest` の復元テストで、3〜12 行目（空行）の `start`・`end` が空（`null` または空文字。`EmployeeForm` の初期値に合わせる）であることを検証する。保存データなしのテストも、12 行すべての 4 項目（名前・休み・開始・終了）を検証する。実装は変えない（テストのみ。実装を一時的に壊して失敗することを確認し、戻す）
  - 対象ファイル：`ShiftControllerTest.java`
  - 完了条件：
    - 3〜12 行目の `start`・`end` を検証するアサーションがある
    - 復元処理で空行の `start` にダミー値を入れるとテストが失敗することを確認し、確認後に元へ戻した（実行ログに書く）
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
- [ ] **T4. 実際の挙動と逆のコメントを直す**
  - 依頼事項：`src/main/java/com/example/shiftmatch/persistence/LatestShiftRepository.java`（60 行目付近）の「入力エラー時に前回の保存を維持するため」というコメントを、実際の理由（`Optional.empty()` は不成立を表し、従業員入力だけを保存して以前の割り当て・スコアを消す）に合わせて直すか、不要なら削除する。コードは変えない。最後に `./mvnw spotless:apply` の後 `./mvnw test` を実行する
  - 対象ファイル：`LatestShiftRepository.java`
  - 完了条件：
    - コメントが実際の挙動と矛盾しない（または削除されている）
    - `./mvnw test` で全テストが成功し、Spotless・Checkstyle の違反が 0 件である
    - `git diff --stat main -- docs AGENTS.md` が空である

## 実行ログ

<!-- implementer が試行結果（失敗理由・リトライ回数）を追記する欄。作成時は空のままにする -->
