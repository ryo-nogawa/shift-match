package com.example.shiftmatch.service;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.DuplicateNameError;
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

  /**
   * 従業員一覧の中から重複する氏名を検出する。
   *
   * <p>氏名が{@code null}またはが{@code isBlank()}である従業員は除外してから重複判定を行う。
   * 重複がない場合は空リストを返す。同じ氏名が3件以上ある場合も1つの{@link DuplicateNameError}にまとめられる。
   *
   * @param employees 従業員一覧
   * @return 重複エラーのリスト。重複がない場合は空リスト
   */
  List<DuplicateNameError> findDuplicateNames(List<Employee> employees);
}
