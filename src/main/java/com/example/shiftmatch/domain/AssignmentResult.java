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

  /**
   * 未出勤者の理由の表示文言を返します。
   *
   * <p>入れる枠があるのに割り当てられなかった人のうち、同じ枠に割り当て済みの人と入れ替えてもずれの合計が変わらない場合は、
   * 5.3 節の同点規則で優先された人の氏名を示します。入れ替えでは同点にならない場合は、より小さいずれの案が選ばれたと示します。
   *
   * @param employee 未出勤者
   * @return 理由の表示文言
   */
  public String unassignedReasonLabel(Employee employee) {
    UnassignedReason reason = employee.unassignedReason();
    if (reason != UnassignedReason.LOWER_GAP_CHOSEN) {
      return reason.label();
    }
    for (ShiftAssignment assignment : assignments) {
      ShiftSlot slot = assignment.slot();
      if (employee.canWork(slot) && employee.gapMinutes(slot) == assignment.gapMinutes()) {
        return "入れる枠はあったが、同じずれの案があり、入力順で優先度が高い " + assignment.employee().name() + " が選ばれた";
      }
    }
    return reason.label();
  }
}
