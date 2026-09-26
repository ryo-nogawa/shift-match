package com.example.shiftmatch.domain;

import java.time.YearMonth;
import java.util.List;

/**
 * 月間シフト作成の入力を表すレコード。
 *
 * <p>対象月、従業員の基本情報、日ごとの個別変更を保持します。
 */
public record MonthlyShiftInput(
    YearMonth month, List<EmployeeProfile> employees, List<ShiftAdjustment> adjustments) {

  /**
   * 従業員リストと個別変更リストを不変にする。
   *
   * <p>呼び出し側がリストを変更しても、レコードの内容に反映されないようにします。
   */
  public MonthlyShiftInput {
    employees = List.copyOf(employees);
    adjustments = List.copyOf(adjustments);
  }
}
