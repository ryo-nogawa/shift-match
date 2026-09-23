# 命名規則

## 対象別の規則

| 対象 | 規則 | 例 |
| --- | --- | --- |
| クラス | UpperCamelCase | `RequestParser` |
| メソッド | lowerCamelCase | `parseRequest` |
| 変数 | lowerCamelCase | `requestBody` |
| 定数 | UPPER_SNAKE_CASE | `MAX_RETRY_COUNT` |
| パッケージ | 全て小文字、単語区切りなし | `welcome` |
| インタフェース | UpperCamelCase（役割を表す名前とする） | `UserRepository` |
| 実装クラス | インタフェース名 + `Impl` | `UserRepositoryImpl` |
| Enum 型 | UpperCamelCase、単数形 | `Status` |
| Enum 定数 | UPPER_SNAKE_CASE | `ACTIVE`, `PENDING_APPROVAL` |
