# コードフォーマット（Spotless / google-java-format）

## 位置づけ

- [Spotless Maven Plugin](https://github.com/diffplug/spotless/tree/main/plugin-maven) 経由で [google-java-format](https://github.com/google/google-java-format)（`GOOGLE` スタイル）を実行し、Java ソースを自動整形します
- 設定は [pom.xml](../../pom.xml) の `spotless-maven-plugin` に記述しています（専用の設定ファイルは持ちません）
- `validate` フェーズに `spotless:check` をバインドしているため、`./mvnw test` を含むほとんどのコマンドで自動的にチェックが走り、未整形のコードがあるとビルドが失敗します
- 同じ `validate` フェーズの [Checkstyle](checkstyle.md) より前に実行されるよう pom.xml 上で先に宣言しています（整形の乱れがそのまま Checkstyle の指摘として二重に出ないようにするため）

## コマンド

```bash
# 自動整形（コミット前に実行する）
./mvnw spotless:apply

# 整形チェックのみ（./mvnw test にも validate フェーズで自動的に含まれる）
./mvnw spotless:check
```

- Checkstyle の違反と異なり、Spotless の違反はほぼ全て `spotless:apply` で自動修正できます。手で整形し直す必要はありません

## Checkstyle との整合性

`config/checkstyle/checkstyle.xml` は Google の `google_checks.xml` をベースにしており、これは google-java-format の出力に合わせて作られた設定です。そのため、`GOOGLE` スタイルを指定するだけで大半のルール（2 スペースインデント、100 桁、波括弧の位置、空行の扱いなど）は自然に一致します。乖離が起き得る点だけ、以下の通り明示的に設定しています。

| 設定 | 値 | 理由 |
| --- | --- | --- |
| `style` | `GOOGLE` | Checkstyle 側の `Indentation`（2 スペース）と一致させるため。`AOSP`（4 スペース）は使用しません |
| `reorderImports` | `true` | Checkstyle の `CustomImportOrder`（static インポートを先頭にまとめ、グループ間に空行）と一致させるため |
| `formatJavadoc` | `false` | Checkstyle 側に日本語 Javadoc 用の独自ルール（[javadoc.md](javadoc.md) の句点「。」、`JavadocParagraph` など）があり、google-java-format の Javadoc 整形と衝突する可能性があるため無効化しています |
| `reflowLongStrings` | `true` | 長い文字列リテラルを 100 桁に収まるよう自動改行し、Checkstyle の `LineLength` 違反を未然に防ぐため |

## Spotless では直せない指摘

google-java-format はホワイトスペース・改行・インポート順序の整形が中心で、意味論的な書き換えは行いません。次のような Checkstyle の指摘は Spotless では自動修正されないため、手動で直す必要があります。

- `AvoidStarImport`（ワイルドカードインポート `import java.util.*;` の展開）
- 命名規則（`LocalVariableName` など）
- `RegexpSinglelineJava` によるプロジェクト独自ルール（メソッド参照禁止・`printStackTrace()` 禁止など、[lambda.md](lambda.md) / [exception.md](exception.md)）
- Javadoc の内容そのもの（`formatJavadoc: false` のため）
