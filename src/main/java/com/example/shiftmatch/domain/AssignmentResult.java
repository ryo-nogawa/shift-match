package com.example.shiftmatch.domain;

import java.util.List;

/**
 * シフト割り当て結果を表すレコード。
 *
 * <p>8 人分の割り当て、ずれの合計（分）、未出勤者を保持します。スコアは「入力時間帯と割り当てた枠の差（ずれ）」の合計で、0
 * が最良です。
 */
public record AssignmentResult(
    List<ShiftAssignment> assignments, int score, List<Employee> unassignedEmployees) {

  /**
   * 各リストを不変なリストとして保持し、不変条件を検証する。
   *
   * <p>生成後に呼び出し側がリストを変更するとスコアと未出勤者の整合性が崩れるため、
   * 生成経路によらず不変なリストにする。
   *
   * @throws IllegalArgumentException {@code assignments} の件数が {@code
   *     ShiftSlot.totalEmployees()} でない場合
   */
  public AssignmentResult {
    if (assignments.size() != ShiftSlot.totalEmployees()) {
      throw new IllegalArgumentException(
          "割り当ては必ずちょうど" + ShiftSlot.totalEmployees() + "件である必要があります");
    }
    assignments = List.copyOf(assignments);
    unassignedEmployees = List.copyOf(unassignedEmployees);
  }

  /**
   * 各割り当てのずれ（分）をリストで返します。
   *
   * <p>{@link #assignments()} と同じ順序・同じ件数で返します。
   *
   * @return 各人のずれのリスト
   */
  public List<Integer> gapMinutesList() {
    return assignments.stream().map(a -> a.gapMinutes()).toList();
  }
}
