# Todo: Codex レビュー 2 回目の指摘対応（休憩時刻 T-1）

- Issue: #10
- ブランチ: feature/10-break-time
- 版: v3（レビュー指摘対応。v1・v2 は完了済み）
- 対象仕様: T-1, F-4
- 作成日: 2026-09-24

## 前提

- 読むべきドキュメント・規約：`.agents/rules/comment.md`、`.agents/rules/test.md`、`.claude/rules/tdd.md`
- 元の指摘：`target/reviews/code-quality-review.md`（MUST 1 件・SHOULD 1 件）
- 実装上の注意：
  - 対象は**今回のブランチで追加・変更したテスト**（`AssignmentResultTest` 全体、`ShiftControllerTest` の休憩表示テスト）のみ。他の既存テストファイルのコメントは変更しない
  - テストの振る舞いは変えない。期待値を仕様と異なる値に変えない（仕様：早番 2 名 13:00〜14:00 / 14:00〜15:00、遅番 2 名 15:00〜16:00 / 16:00〜17:00）

## Todo

- [ ] **T1. [T-1] 追加テストの「何をしているか」の言い換えコメントを削除する**
  - 依頼事項：
    - `AssignmentResultTest` の `// Given` / `// When` / `// Then` / `// Then: 早番1人目` などの言い換えコメントをすべて削除する（空行でブロックを区切ってよい）
    - `ShiftControllerTest` の休憩表示テスト内の `// 行 1`、`// ...を検証` などの言い換えコメントを削除する。正規表現が `<span>`・改行・空白を許容する理由など、コードから分からない「なぜ」だけはコメントとして残してよい
  - 対象ファイル：`src/test/java/com/example/shiftmatch/domain/AssignmentResultTest.java`、`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `grep -n "// Given\|// When\|// Then" src/test/java/com/example/shiftmatch/domain/AssignmentResultTest.java` が 0 件
    - `ShiftControllerTest` の休憩表示テストに、処理内容を言い換えただけのコメントが残っていない
    - `./mvnw test -Dtest=AssignmentResultTest,ShiftControllerTest` が成功する
- [ ] **T2. [T-1, F-4] 休憩表示テストの行検証を「1 行の `<tr>` の範囲内」に限定する**
  - 依頼事項：
    - 先に「壊れた表示でも通ってしまう」ことを示す RED を作る：例えば、太郎の休憩セルを空にし、後続行に `13:00〜14:00` がある本文でも現状の検証が通ってしまうことを確認する（一時的なテストでよい。確認後は消す）。または、検証ロジック修正前に、`</tr>` を越えない検証に切り替えると期待どおり失敗する状況を作って確認する
    - 結果表の `<tr>...</tr>` を 1 行ずつ抽出（`Pattern.compile("<tr>.*?</tr>", DOTALL)` で本文の**「割当結果」見出し以降**を対象にする）し、抽出した行のリスト内で、枠・氏名・休憩範囲が同じ行に含まれることを検証する。正規表現が `</tr>` を越えて一致しないようにする
    - 行順は、氏名の全文検索位置（`indexOf`）ではなく、抽出した結果行リストの順序（0 番目＝早番 1 人目 13:00〜14:00、1 番目＝早番 2 人目 14:00〜15:00、2 番目＝遅番 1 人目 15:00〜16:00、3 番目＝遅番 2 人目 16:00〜17:00）で検証する。入力フォームに同じ氏名が描画されても影響を受けないこと
  - 対象ファイル：`src/test/java/com/example/shiftmatch/controller/ShiftControllerTest.java`
  - 完了条件：
    - `</tr>` を越えて一致しない検証に置き換わっている（`.*?` と DOTALL を使う場合でも 1 行の `<tr>...</tr>` に限定して抽出している）
    - 順序検証に `body.indexOf("太郎")` のような氏名の全文検索位置を使っていない
    - `./mvnw test -Dtest=ShiftControllerTest` が成功する
- [ ] **T3. 全体テストと静的解析を確認する**
  - 依頼事項：`./mvnw spotless:apply` の後、`./mvnw test` を全体で実行し、実行ログに結果を追記する
  - 完了条件：`./mvnw test` で全テストが成功する（Spotless・Checkstyle 違反 0 件）

## 実行ログ

<!-- implementer が試行結果を追記する欄 -->
