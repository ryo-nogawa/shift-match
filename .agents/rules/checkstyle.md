# Checkstyle

## 位置づけ

- `google_checks.xml`（Checkstyle 14.1.0）をベースに、本プロジェクト向けにカスタマイズした静的解析です
- 設定ファイル：[config/checkstyle/checkstyle.xml](../../config/checkstyle/checkstyle.xml)
- 抑制ルール：[config/checkstyle/checkstyle-suppressions.xml](../../config/checkstyle/checkstyle-suppressions.xml)（`src/test` 配下の Javadoc 関連チェックと `TypeName` を除外）
- `validate` フェーズにバインドされているため、`./mvnw test` を含むほとんどのコマンドで自動的に実行され、違反があるとビルドが失敗します
- 単体で実行する場合は `./mvnw checkstyle:check`

## google_checks.xml からの変更点

| 変更 | 理由 |
| --- | --- |
| `MissingJavadocMethod` / `MissingJavadocType` の `scope` を `protected` → `package` に変更 | public・protected・package-private を対象、private を対象外とする [javadoc.md](javadoc.md) に合わせるため |
| `SummaryJavadoc` に `period="。"` を追加 | 日本語の Javadoc は句点（。）で終わるため（Google 標準の `.` のままだと日本語の要約が常に違反になる） |
| `ConstantName` を追加 | google_checks.xml には定数（static final フィールド）の命名チェックが含まれていないため、[naming.md](naming.md) の「定数は UPPER_SNAKE_CASE」を補うために追加 |
| `RegexpSinglelineJava`（id: `BanMethodReference`）を追加 | [lambda.md](lambda.md) の「メソッド参照禁止」を自動検知するため。`Type::method` 形式の文字列パターンで検出する簡易チェックであり、AST ベースの厳密な検査ではない |
| `RegexpSinglelineJava`（id: `BanPrintStackTrace`）を追加 | [exception.md](exception.md) の「`e.printStackTrace()` 禁止」を自動検知するため |
| インデント・タブ文字禁止（`FileTabCharacter`）・1 行 100 文字（`LineLength`） | Google 標準のまま変更していません。既存の `.java` ファイルはスペース 2 つに変換済みです |

## Checkstyle では検出できない規約（レビューで確認する）

以下は意味論的な判断が必要なため、Checkstyle では自動検知できません。Codex のコードレビュー（`.agents/agents/code-quality-review.md`）または人手のレビューで確認してください。

- コメントが「何をしているか」ではなく「なぜこの実装にしたか」を記載しているか（[comment.md](comment.md)）
- 実装クラスの命名（インタフェース名 + `Impl`）、Enum 定数の命名（[naming.md](naming.md)）
- 環境依存値のハードコード禁止（[config.md](config.md)）
- チェック例外のみを `try-catch` で囲んでいるか（[exception.md](exception.md)）
- スレッドセーフ性（共有状態への非スレッドセーフなコレクションの使用など、[thread-safety.md](thread-safety.md)）

## 個別の抑制

特定の 1 行だけ抑制したい場合は、抑制ファイルを編集せず、対象行の直前にコメントを置きます。

```java
// CHECKSTYLE.SUPPRESS: BanMethodReference for +1 lines
Comparator<String> c = String::compareTo;
```

安易な抑制は規約違反を隠すことになるため、使用する場合は理由をコミットメッセージまたはコードコメントに残してください。
