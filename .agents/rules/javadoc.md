# Javadoc

- 別クラスから参照できるクラス・メソッドに Javadoc を記載する
  - 対象：`public`、`protected`、パッケージプライベート（修飾子なし）
  - 対象外：`private` のクラス・メソッド
- テストコードには Javadoc を記載しなくてよい

## 記載ルール

- 1 行目にタイトル（概要）を記載する
- 2 行目以降に実装内容を記載する
  - 実装内容は `<p>` タグで段落を区切って記載する
  - タイトルと `<p>` の間、および `<p>` 同士の間には必ず 1 行の空行を入れる
- 別クラス・別メソッドを参照する場合は `{@link}` を使用する
- コード（クラス名、変数名、サンプルコードなど）を記載する場合は `{@code}` を使用する

### 記載例

```java
/**
 * リクエストをパースするクラス。
 *
 * <p>受信したバイト列を{@link HttpRequest}に変換する。
 * 変換に失敗した場合は{@code null}を返す。
 */
public class RequestParser {

    /**
     * 入力ストリームからHTTPリクエストを読み取る。
     *
     * <p>読み取ったデータは{@link #parse(String)}に渡してパースする。
     *
     * @param input 入力ストリーム
     * @return パース済みの{@link HttpRequest}
     */
    public HttpRequest read(InputStream input) {
        // ...
    }
}
```
