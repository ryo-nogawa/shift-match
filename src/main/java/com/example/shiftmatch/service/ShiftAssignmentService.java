package com.example.shiftmatch.service;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.Employee;
import java.util.List;
import java.util.Optional;

/**
 * シフト割り当てを行うサービスインターフェース。
 *
 * <p>従業員の希望をもとに、ハード制約を満たし、スコアを最大化する割り当て案を算出します。
 */
public interface ShiftAssignmentService {

  /**
   * 従業員一覧をもとにシフト割り当てを行う。
   *
   * @param employees 従業員一覧
   * @return 割り当て結果。条件を満たす案がない場合は空の Optional を返す
   */
  Optional<AssignmentResult> assign(List<Employee> employees);
}
