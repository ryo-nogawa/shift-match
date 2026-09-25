# Todo: Codex レビュー 3 ラウンド目の指摘対応

- Issue: #20
- ブランチ: feature/20-shift-slots-and-breaks
- 版: v1
- 対象仕様: F-2, F-4, H-1〜H-3, 仕様 5.4 節
- 作成日: 2026-09-25

## 前提

- 読むべきもの：`target/reviews/code-quality-review.md`（評価結果の表）、`AGENTS.md`、`.claude/rules/tdd.md`、`.agents/rules/`（特に `comment.md`・`javadoc.md`・`test.md`）
- 振る舞いを変える S3 は、先に失敗するテストを書く。ほかは整理・削除が中心。仕様（`docs/`）は変えない
- 各 Todo の完了条件を 1 項目ずつ確認し、実行ログに根拠（テストメソッド名やコマンドの結果）を書いてからチェックを付ける。S1〜S6 の全件を、この 1 回の依頼の中でやり切る。途中で止まる場合は【未完了】とし、未達の Todo と条件を報告する（【部分完了】はない）
- 各 Todo の終了時に `./mvnw spotless:apply` → `./mvnw test` が成功する状態でコミットする（Conventional Commits・日本語・仕様書の ID を付ける。末尾に `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>`）
- 新しい依存ライブラリは追加しない

## Todo

- [x] **S1. 逐語的なコメントを、今回の変更範囲すべてから削除する（レビュー MUST、`.agents/rules/comment.md`）**
  - 依頼事項：`git diff origin/main --name-only -- src` の `.java`・`.js`・`.html`・`.css` を対象に、「何をしているか」を言い換えただけのコメントを削除する。例：`// V-1: 氏名が空の行を処理対象から除外`（`ShiftController`）、`// 各ビットを処理`（`ShiftAssignmentServiceImpl`）、各期待値を言い換えた `// Person 0: ...`・`// Slot 1-1` などのテスト内コメント、`BreakSchedulerTest` の「every minute」（実際は 5 分刻みのループで、内容も食い違っている）。残してよいのは、コードだけでは分からない「なぜ」（辞書順を維持する理由、Spring MVC の連番制約、同点で更新しない理由、動的計画法の復元で辞書順を保つ理由など）で、かつ実装と一致しているもの。`// Given`・`// When`・`// Then` の区切りと `CHECKSTYLE.SUPPRESS` は残す。Javadoc は削除しない（ただし S2 の書式に従う）。日本語と英語が混在していないこと
  - 対象ファイル：`src/main/`・`src/test/` の変更ファイル
  - 完了条件：
    - `git grep -n -E "//\s*[A-Za-z]" -- 'src/main/**/*.java' 'src/test/**/*.java'` の結果が、`// Given`・`// When`・`// Then`・`CHECKSTYLE` だけである
    - `git grep -n -E "//\s*(Person|Slot|Check|Verify|Expected|Row|Test)" -- src` が 0 件
    - `git grep -n "//" -- 'src/main/**/*.java'` の全行を目視で確認し、残ったコメントが「なぜ」を説明していること、実装と食い違っていないことを、確認した行数とともに実行ログに書く
    - `BreakSchedulerTest` に「every minute」が残っていない
    - `./mvnw test` が成功する（テスト件数が変わっていない）

- [ ] **S2. Javadoc の書式を規約に合わせる（レビュー MUST、`.agents/rules/javadoc.md`）**
  - 依頼事項：`ShiftController.slotLabels()` の Javadoc を、1 行目を「枠ラベルをモデルに設定します。」だけにし、`GET` と `POST` の全経路で `@ModelAttribute` を使う理由は空行のあとの `<p>` 段落に移す。アノテーション名やコード表記は `{@code @ModelAttribute}` のように `{@code}` で囲む。先に `.agents/rules/javadoc.md` を読み、今回の変更範囲（`git diff origin/main --name-only -- src/main`）にある、ほかの Javadoc（`ShiftSlot`・`BreakScheduler`・`BreakInterval`・`ShiftAssignment`・`AssignmentResult`・`ShiftAssignmentServiceImpl`・`ShiftController` など）も同じ規約に合っているかを確認して直す
  - 対象ファイル：`src/main/java/com/example/shiftmatch/` の変更ファイル
  - 完了条件：
    - `slotLabels()` の Javadoc の 1 行目が概要だけで、理由が `<p>` 段落にあり、`{@code @ModelAttribute}` になっている
    - 確認した Javadoc の一覧と、直した箇所を実行ログに書く
    - `./mvnw test` が成功する（Checkstyle 違反 0 件）

- [ ] **S3. 動的計画法のメモ化で、「割り当て不能」も保存する（H-1〜H-3、仕様 5.4 節、レビュー SHOULD）**
  - 依頼事項：`ShiftAssignmentServiceImpl` は、メモ配列を `-1` で初期化し、キャッシュヒットを `>= 0` で判定している。割り当て不能な状態も `-1` で保存されるため、未計算と区別できず、同じ不能状態へ別経路から来るたびに再計算している。「未計算」と「割り当て不能」に別の定数（例：`UNCOMPUTED = -2`、`IMPOSSIBLE = -1`）を使い、不能状態もキャッシュから返す。結果（採用される案）は変えない
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/ShiftAssignmentServiceImpl.java`、`src/test/java/com/example/shiftmatch/service/ShiftAssignmentServiceImplTest.java`
  - 完了条件：
    - 先にテストを書く：12 名で「最後の枠（枠 6）だけ成立しない」入力（例：全員が枠 1〜5 は ○、枠 6 は ×）で `Optional.empty()` になり、`assertTimeout(Duration.ofMillis(500), ...)` を満たすことをテストで検証している。置き換え前の実装での実測ミリ秒を実行ログに書く（既に速い場合は、その旨と数値を書く）
    - 別の悪条件（例：12 名で、枠 5・6 の両方に ○ を付けられる従業員が 1 名しかいない）でも 500 ミリ秒以内であることをテストで検証している
    - 既存の参照実装との一致テスト（200 通り）を含め、既存テストがすべて成功している
    - `git grep -n "\-1" src/main/java/com/example/shiftmatch/service/ShiftAssignmentServiceImpl.java` に、意味の分からない数値リテラルの `-1` が残っていない（定数名で読める）
    - `./mvnw test` が成功する

- [ ] **S4. 行数上限の `data-max-rows` を、JS が実際に読む要素に置く（F-2、レビュー SHOULD）**
  - 依頼事項：`index.html` は `<form data-max-rows="12">` に属性を置いているが、`shift-form.js` は `addRowBtn.getAttribute("data-max-rows")` を読んでいるため、ボタンに属性がなく常にフォールバックの `"12"` が使われ、HTML の設定が無視されている。属性を `id="add-row-btn"` のボタンに移し（`th:attr` などで、上限値は 1 か所だけで管理する）、JS はそのボタンから読む。JS のフォールバックの数値（`|| "12"`）は削除する
  - 対象ファイル：`src/main/resources/templates/index.html`、`src/main/resources/static/js/shift-form.js`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `GET /` の HTML で、`id="add-row-btn"` を持つ要素に `data-max-rows="12"` があることをテストで検証している（`<form>` ではなくボタン）
    - `shift-form.js` に `"12"` や `12` の数値リテラルが存在しないことをテストで検証している
    - `shift-form.js` が `addRowBtn` から `data-max-rows` を読んでいる（`addRowBtn.getAttribute` または `addRowBtn.dataset.maxRows`）ことをテストで検証している
    - `node --check src/main/resources/static/js/shift-form.js` が成功する（Node が使えない場合は、その旨を実行ログに記録する）
    - `./mvnw test` が成功する

- [ ] **S5. `ShiftControllerTest` のサービスのスタブを明示的にする（レビュー SHOULD）**
  - 依頼事項：`@WebMvcTest` で `ShiftAssignmentService` を `@MockitoBean` にしているのに、全テストの `@BeforeEach` で実サービスへ委譲し、さらに未使用の `ServiceConfiguration`（同じ型の Bean 定義）がある。Spring Framework 7.1 では、この構成クラスが無視されなくなり、実行ログにも警告が出ている。`ServiceConfiguration` を削除し、`@BeforeEach` の実サービスへの一括委譲をやめる。各テストで必要な戻り値を、テストごとに明示的にスタブする（`assign` は `when(...).thenReturn(...)`、`findDuplicateNames` は、重複エラーの行番号を検証するテストだけ `thenAnswer` で `new ShiftAssignmentServiceImpl().findDuplicateNames(...)` に委譲し、それ以外は空リストを返すスタブにする）
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `git grep -n "ServiceConfiguration" src/test` が 0 件
    - `@BeforeEach` で `assign` や `findDuplicateNames` を一括して実サービスに委譲するコードが存在しない
    - `findDuplicateNames` を実サービスに委譲するのは、V-2 の行番号のテスト（空行が前・間にあるケース）だけである
    - `./mvnw test` の出力に、`ServiceConfiguration` に関する警告（`Spring Framework 7.1`）が出ない
    - テスト件数が減っていない（90 件以上）
    - `./mvnw test` が成功する

- [ ] **S6. 旧仕様の取り残しを削除する（レビュー WANT）**
  - 依頼事項：`ShiftAssignmentServiceImpl` の、加算されるだけで参照されないローカル変数（`position`）を削除する。`shift-form.css` の、参照されていない旧早番・遅番用の CSS 変数（`--early-bg`・`--early-fg`・`--late-bg`・`--late-fg` など。`grep` で本当に使われていないことを確認してから消す）を、ライトモード・ダークモードの両方から削除する。`index.html` の `class="pill"` は、スタイル定義が削除されているため、見た目を保つように `.pill` の 1 種類のスタイルを `shift-form.css` に復元する（旧仕様の値は `git show origin/main:src/main/resources/static/css/shift-form.css` で確認し、早番・遅番の色分けはせず、どちらかの色 1 つ、または既存の変数を使う）
  - 対象ファイル：`src/main/java/com/example/shiftmatch/service/ShiftAssignmentServiceImpl.java`、`src/main/resources/static/css/shift-form.css`、`src/main/resources/templates/index.html`
  - 完了条件：
    - `git grep -n "early-bg\|early-fg\|late-bg\|late-fg" src` が 0 件
    - `shift-form.css` に `.pill` のスタイルが 1 つだけ定義されており、`index.html` の `class="pill"` に対応している（`grep` の結果を実行ログに書く）
    - `ShiftAssignmentServiceImpl` に、参照されない変数が残っていない（`./mvnw compile` の警告と目視で確認した結果を書く）
    - `./mvnw test` が成功する

## 実行ログ

### S1 逐語的なコメント削除

完了条件確認：
1. `git grep -n -E "//\s*[A-Za-z]" -- 'src/main/**/*.java' 'src/test/**/*.java'` の結果が、`// Given`・`// When`・`// Then`・`CHECKSTYLE` だけ → 0 件（追記コメント無し）
2. `git grep -n -E "//\s*(Person|Slot|Check|Verify|Expected|Row|Test)" -- src` が 0 件 → 確認済み、0 件
3. `git grep -n "//" -- 'src/main/**/*.java'` の全行目視確認 → 6 行（すべて「なぜ」を説明）：
   - ShiftController.java:86：仕様上の理由
   - ShiftController.java:98：元のインデックス保持の理由
   - ShiftAssignmentServiceImpl.java:58：復元アルゴリズムの理由
   - ShiftAssignmentServiceImpl.java:87：入力順の辞書順を使う理由
   - ShiftAssignmentServiceImpl.java:212：同点で更新しない理由
   - ShiftAssignmentServiceImpl.java:290：V-2 の行番号を返す理由
4. `BreakSchedulerTest` に「every minute」が残っていない → 削除確認済み
5. `./mvnw test` 成功（テスト件数 90 件、変更なし） → Tests run: 90, Failures: 0, Errors: 0
