# スレッドセーフ性

## 基本方針

- 複数スレッドから共有されうる状態（フィールド、静的変数、コレクションなど）は、スレッドセーフな実装にする
- 通常の `HashMap`、`ArrayList` などの非スレッドセーフなコレクションは、複数スレッドから書き込みが発生しうる箇所では使用しない
  - 用途に応じて `java.util.concurrent` パッケージのクラスに置き換える
- 複数のフィールドにまたがる操作など、`java.util.concurrent` パッケージのクラスだけでは排他制御できない場合は、`synchronized` を使用する

### 使用するクラスの例

| 用途 | 置き換え先の例 |
| --- | --- |
| Map | `ConcurrentHashMap` |
| カウンタなどの数値 | `AtomicInteger`、`AtomicLong` |
| リスト | `CopyOnWriteArrayList` |
| キュー | `ConcurrentLinkedQueue`、`BlockingQueue` |

### 記載例

```java
// NG：複数の接続スレッドから書き込まれる可能性があるのに HashMap を使用
private final Map<String, Integer> connectionCountMap = new HashMap<>();

// OK：ConcurrentHashMap に置き換える
private final Map<String, Integer> connectionCountMap = new ConcurrentHashMap<>();
```

## synchronized を使う場合

- 複数のフィールドの整合性を保ちながら更新する必要がある場合など、単一のクラスでは対応できない複合的な処理を排他制御する場合に使用する
- 排他制御の範囲はできるだけ狭くし、メソッド全体ではなく該当処理のみを `synchronized` ブロックで囲む

### 記載例

```java
private int activeCount;
private int totalCount;

// NG：activeCount と totalCount の整合性が取れないまま他スレッドから参照される可能性がある
public void onConnect() {
    activeCount++;
    totalCount++;
}

// OK：2 つのフィールドの更新をまとめて排他制御する
private final Object lock = new Object();

public void onConnect() {
    synchronized (lock) {
        activeCount++;
        totalCount++;
    }
}
```
