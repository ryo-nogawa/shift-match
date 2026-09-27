package com.example.shiftmatch.domain;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 営業日ごとのシフト算出結果を表すレコード。
 *
 * <p>割り当て結果がない場合（不成立の日）は {@code assignment} が空になり、{@code failureReason}（6 章）が不成立の理由を示します。
 * 成立した日は {@code failureReason} が {@code null} になります。
 */
public record DailyShiftResult(
    LocalDate date,
    int availableCount,
    Optional<AssignmentResult> assignment,
    FailureReason failureReason) {

  /**
   * 旧シグネチャのコンストラクタ（不成立の理由を既定値で補う）。互換性のために残しています。
   *
   * <p>{@code assignment} が空なら {@link FailureReason#STAFF_SHORTAGE}、成立なら {@code null} を補います。
   *
   * @param date 対象の日付
   * @param availableCount 勤務できる人数
   * @param assignment 割り当て結果（不成立なら空）
   */
  public DailyShiftResult(
      LocalDate date, int availableCount, Optional<AssignmentResult> assignment) {
    this(
        date,
        availableCount,
        assignment,
        assignment.isEmpty() ? FailureReason.STAFF_SHORTAGE : null);
  }

  /**
   * {@code assignment} と {@code failureReason} の整合性を検証します。
   *
   * @throws IllegalArgumentException 割り当て結果があるのに {@code failureReason} が {@code null}
   *     でない場合、または割り当て結果がないのに {@code failureReason} が {@code null} の場合
   */
  public DailyShiftResult {
    if (assignment.isPresent() && failureReason != null) {
      throw new IllegalArgumentException("割り当て結果がある場合、failureReasonはnullである必要があります");
    }
    if (assignment.isEmpty() && failureReason == null) {
      throw new IllegalArgumentException("割り当て結果がない場合、failureReasonはnullであってはいけません");
    }
  }
}
