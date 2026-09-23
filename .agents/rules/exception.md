# 例外処理

- 過剰なエラーハンドリングは行わない
- チェック例外（`Exception` のサブクラスのうち `RuntimeException` 系を除くもの）が発生しうるメソッドのみ `try-catch` で囲む
- リソース系（`Closeable` / `AutoCloseable` を実装するクラスなど）の例外が発生しうる場合は、`try-with-resources` を使用する
- 例外を捕捉した際は、必ずスタックトレースを出力する
  - 出力には SLF4J のロガーを使用し、`e.printStackTrace()` は使用しない
  - ロガーの第 2 引数に例外オブジェクトを渡し、スタックトレースを出力する

## 記載例

```java
private static final Logger LOGGER = LoggerFactory.getLogger(RequestParser.class);

// NG：標準エラー出力に直接出力している
try {
    // ...
} catch (IOException e) {
    e.printStackTrace();
}

// OK：SLF4J のロガーで、例外オブジェクトを渡して出力している
try {
    // ...
} catch (IOException e) {
    LOGGER.error("リクエストの読み取りに失敗しました", e);
}
```
