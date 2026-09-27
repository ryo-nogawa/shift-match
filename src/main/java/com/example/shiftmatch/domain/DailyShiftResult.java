package com.example.shiftmatch.domain;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 営業日ごとのシフト算出結果を表すレコード。
 *
 * <p>割り当て結果がない場合（不成立の日）は {@code assignment} が空になり、{@code failureReason} に理由が入ります。
 */
public record DailyShiftResult(
    LocalDate date,
    int availableCount,
    Optional<AssignmentResult> assignment,
    Optional<FailureReason> failureReason) {

  /**
   * 成立と理由の整合を検査する。
   *
   * @throws IllegalArgumentException 成立なのに理由がある、または不成立なのに理由がない場合
   */
  public DailyShiftResult {
    if (assignment.isPresent() && failureReason.isPresent()) {
      throw new IllegalArgumentException("成立した日に不成立の理由は指定できません");
    }
    if (assignment.isEmpty() && failureReason.isEmpty()) {
      throw new IllegalArgumentException("不成立の日には理由が必要です");
    }
  }

  /**
   * 不成立の理由を人員不足として扱う簡易コンストラクタ。
   *
   * @param date 営業日
   * @param availableCount 勤務できる人数
   * @param assignment 割り当て結果。不成立なら空
   */
  public DailyShiftResult(
      LocalDate date, int availableCount, Optional<AssignmentResult> assignment) {
    this(
        date,
        availableCount,
        assignment,
        assignment.isPresent() ? Optional.empty() : Optional.of(FailureReason.SHORTAGE));
  }
}
