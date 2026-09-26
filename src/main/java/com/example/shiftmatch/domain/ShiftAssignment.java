package com.example.shiftmatch.domain;

import java.time.LocalTime;

/**
 * 1 人分のシフト割り当てを表すレコード。
 *
 * <p>従業員、割り当てられた枠、休憩の開始・終了時刻を保持します。
 */
public record ShiftAssignment(
    Employee employee, ShiftSlot slot, LocalTime breakStart, LocalTime breakEnd) {

  /**
   * この割り当てにおける「ずれ」（入力時間帯と割り当てた枠の勤務時間の差）を返します。
   *
   * @return ずれ（分）
   */
  public int gapMinutes() {
    return employee.gapMinutes(slot);
  }
}
