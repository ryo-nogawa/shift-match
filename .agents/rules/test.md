# テスト

## 使用ライブラリ

- JUnit 5
- Mockito

## テスト種別

- 単体テスト：対象の Java ファイル（クラス）単位で作成する
- 結合テスト：`integration` パッケージ（フォルダ）を作成し、通信プロトコルごとに別ファイルでテストコードを実装する
  - HTTP 用のテストファイル

## 必須の確認観点

- 正常系だけでなく、エラーハンドリングの確認も必ず実装する

## カバレッジ観点

以下のいずれか 1 つ以上を満たすこと（複数選択可）。

- 命令網羅
- 判定網羅
- 条件網羅
- 複数条件網羅（ただし 2 要素間の網羅性まで検証すればよく、3 要素以上の組み合わせは不要）

## 命名規則

- `@Nested` を使ってテストをグルーピングすること（正常系、異常系、Exception など）
- `@DisplayName` を使って、どのようなテストなのかを Given-When-Then パターンで日本語により明記すること
  - 仕様に対応するテストは、`@DisplayName` の先頭に仕様 ID を `[V-1]` の形式で付けること（複数ある場合は `[V-1][H-3]`）
- 各テストメソッド名は lowerCamelCase とし、`@DisplayName` に記載した内容を捉えたメソッド名にすること

### 記載例

```java
@Nested
class 正常系 {

    @Test
    @DisplayName("Given: 有効なHTTPリクエストが与えられたとき, When: パースを実行すると, Then: HttpRequestオブジェクトが返る")
    void returnsHttpRequestWhenValidRequestIsParsed() {
        // ...
    }
}

@Nested
class 異常系 {

    @Test
    @DisplayName("Given: 不正な形式のリクエストが与えられたとき, When: パースを実行すると, Then: nullが返る")
    void returnsNullWhenRequestFormatIsInvalid() {
        // ...
    }
}

@Nested
class Exception {

    @Test
    @DisplayName("Given: 入力ストリームがnullのとき, When: パースを実行すると, Then: NullPointerExceptionがスローされる")
    void throwsNullPointerExceptionWhenInputStreamIsNull() {
        // ...
    }
}
```

仕様 ID を付ける場合の例：

```java
@Test
@DisplayName("[V-1] Given: 従業員名が空の行があるとき, When: 入力チェックを実行すると, Then: その行は処理対象から除外される")
void excludesRowWhenEmployeeNameIsBlank() {
    // ...
}
```

## Javadoc の扱い

- テストクラス・テストメソッド（`@Nested` 内部クラスを含む）は [javadoc.md](javadoc.md) の対象外とする
  - 理由：`@DisplayName` で Given-When-Then パターンによりテスト内容を明記するため、Javadoc での説明は不要（二重管理を避ける）
  - Checkstyle 導入時は、`config/checkstyle/checkstyle-suppressions.xml` で `src/test` 配下の Javadoc 関連チェックを除外する

## アサーション

- 使用可能なのは標準ライブラリ（`org.junit.jupiter.api.Assertions`）と Lombok のみとする
- AssertJ などのアサーション用ライブラリは追加導入しない

## テストの独立性

- 各テストは実行順序や他テストの結果に依存しないこと
- フィールドで状態を共有せず、`@BeforeEach` で毎回初期化すること

## 結合テストにおけるリソース管理

- Tomcat など起動コストの高いサーバー本体は、テストクラスにつき `@BeforeAll` で 1 回だけ起動し、`@AfterAll` で 1 回だけ停止する（テストメソッド毎に起動・停止はしない）
- サーバー本体は使い回す一方、接続・リクエストなど各テストメソッド固有の状態はテスト間で引き継がれないよう `@BeforeEach` で毎回作り直す（例：HTTP クライアントの生成、送信するリクエスト内容など）
- `@BeforeAll` / `@AfterAll` をインスタンスメソッドとして定義する場合は、クラスに `@TestInstance(Lifecycle.PER_CLASS)` を付与すること（付与しない場合、`@BeforeAll` / `@AfterAll` は static メソッドである必要がある）
