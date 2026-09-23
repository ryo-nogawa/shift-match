# 環境依存値の外部化

## 基本方針

- ホスト名、ポート番号、パスなど、実行環境によって値が変わりうるものはソースコードに直接記述しない
- 環境依存値は `src/main/resources/application.properties` に定義し、コードからは `@Value` でプロパティとして注入する

## 対象となる値の例

- ポート番号
- ホスト名・IP アドレス
- タイムアウト時間
- スレッドプールのサイズ
- ファイルパス

## 記載例

```properties
# application.properties
tcp.server.port=8081
tcp.server.timeoutMillis=30000
```

```java
@Value("${tcp.server.port}")
private int port;

@Value("${tcp.server.timeoutMillis}")
private long timeoutMillis;
```

- 上記のような値をコード中に `8081` や `30000` のようなリテラルで直接記述しない
