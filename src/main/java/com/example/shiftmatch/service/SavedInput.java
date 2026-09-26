package com.example.shiftmatch.service;

import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.ShiftAdjustment;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * 保存済みの入力をまとめたレコード。
 *
 * @param employees 保存済みの従業員（保存順。未保存なら空）
 * @param adjustments 保存済みの個別変更（未保存なら空）
 * @param lastTargetMonth 最後に保存した対象月（未保存なら空）
 */
public record SavedInput(
    List<EmployeeProfile> employees,
    List<ShiftAdjustment> adjustments,
    Optional<YearMonth> lastTargetMonth) {

  /** リストを不変にします。 */
  public SavedInput {
    employees = List.copyOf(employees);
    adjustments = List.copyOf(adjustments);
  }
}
