package com.example.shiftmatch.domain;

import java.util.List;

/**
 * シフト割り当て結果を表すレコード。
 *
 * <p>早番と遅番に割り当てられた従業員、スコア、未出勤者を保持します。
 */
public record AssignmentResult(
    List<Employee> earlyEmployees,
    List<Employee> lateEmployees,
    int score,
    List<Employee> unassignedEmployees) {

  /**
   * 各リストを不変なリストとして保持する。
   *
   * <p>生成後に呼び出し側がリストを変更すると早番・遅番・スコアと未出勤者の整合性が崩れるため、
   * 生成経路によらず不変なリストにする。
   */
  public AssignmentResult {
    earlyEmployees = List.copyOf(earlyEmployees);
    lateEmployees = List.copyOf(lateEmployees);
    unassignedEmployees = List.copyOf(unassignedEmployees);
  }
}
