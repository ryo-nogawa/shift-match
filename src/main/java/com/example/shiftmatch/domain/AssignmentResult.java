package com.example.shiftmatch.domain;

import java.util.List;

/**
 * シフト割り当て結果を表すレコード。
 *
 * <p>8 人分の割り当て、スコア、未出勤者を保持します。
 */
public record AssignmentResult(
    List<ShiftAssignment> assignments, int score, List<Employee> unassignedEmployees) {

  /**
   * 各リストを不変なリストとして保持し、不変条件を検証する。
   *
   * <p>生成後に呼び出し側がリストを変更するとスコアと未出勤者の整合性が崩れるため、
   * 生成経路によらず不変なリストにする。
   *
   * @throws IllegalArgumentException {@code assignments} の件数が
   *     ちょうど 8 件でない場合
   */
  public AssignmentResult {
    if (assignments.size() != 8) {
      throw new IllegalArgumentException("割り当ては必ずちょうど8件である必要があります");
    }
    assignments = List.copyOf(assignments);
    unassignedEmployees = List.copyOf(unassignedEmployees);
  }
}
