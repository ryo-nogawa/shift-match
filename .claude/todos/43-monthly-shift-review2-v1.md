# Todo: 月間シフト算出 Codex レビュー 2 ラウンド目の指摘対応

- Issue: #43
- PR: #48
- 対象仕様: V-4・F-5

## 前提

Codex レビュー 2 ラウンド目の指摘に対応します。詳細は `/Users/ryonogawa/Develop/java/projects/shift-match/target/reviews/code-quality-review.md` を参照してください。

## 依頼事項

`MonthlyShiftServiceImplTest.java` の V-4 のテスト（163 行目付近。8 名勤務可能な 1 日目と 7 名の 2 日目を用意しているテスト）で、`assignmentService.assign` を 1 日目では成立する結果、2 日目では空を返すようスタブし、日別独立の振る舞いを検証してください。

## 完了条件

- [x] T1：V-4 のテストで `assignmentService.assign` を呼び出し順に基づいてスタブし、1 日目は具体的な `Optional<AssignmentResult>`（成立する結果）、2 日目は `Optional.empty()` を返す
- [x] T2：1 日目の `assignment().isPresent()` を検証
- [x] T3：2 日目の `assignment().isEmpty()` を検証（既存）
- [x] T4：`verify(assignmentService, times(2)).assign(anyList())` で両日分の呼び出しを検証
- [x] T5：`./mvnw test` で全テスト成功（Spotless・Checkstyle を含む）

## 実行ログ

- T1 完了：スタブ設定済み、`createDummyAssignmentResult()` でダミーの `Optional<AssignmentResult>` を返す
- T2 完了：1 日目の `assignment().isPresent()` アサーション追加済み
- T3 既存：2 日目の `assignment().isEmpty()` アサーション既に存在
- T4 完了：`verify(assignmentService, times(2)).assign(anyList())` 追加済み
- T5 完了：./mvnw test で全テスト成功（351 件、エラーなし）
