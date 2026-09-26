package com.example.shiftmatch.persistence;

import com.example.shiftmatch.domain.MonthlyShiftResult;
import java.util.List;

/**
 * 保存済みの月間シフトを表すレコード。
 *
 * <p>決定したシフトと、その時点の従業員名（入力順）を保持します。
 *
 * @param result 月間シフトの結果
 * @param employeeNames シフトを作成した時点の従業員名（入力順）
 */
public record SavedMonthlyShift(MonthlyShiftResult result, List<String> employeeNames) {

  /** 従業員名のリストを不変にします。 */
  public SavedMonthlyShift {
    employeeNames = List.copyOf(employeeNames);
  }
}
