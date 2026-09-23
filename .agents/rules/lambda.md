# ラムダ式

## 基本方針

- 関数型インタフェース（`Runnable`、`Comparator`、`Consumer` など）の実装はラムダ式で記述する
- メソッド参照（`ClassName::methodName` 形式）は使用しない

## 記載例

```java
// OK：ラムダ式
executor.submit(() -> handleConnection(socket));

// NG：メソッド参照
executor.submit(this::handleConnection);
```
