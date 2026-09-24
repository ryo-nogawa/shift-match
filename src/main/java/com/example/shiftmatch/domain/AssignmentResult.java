package com.example.shiftmatch.domain;

import java.time.LocalTime;
import java.util.ArrayList;
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
   * 各リストを不変なリストとして保持し、不変条件を検証する。
   *
   * <p>生成後に呼び出し側がリストを変更すると早番・遅番・スコアと未出勤者の整合性が崩れるため、
   * 生成経路によらず不変なリストにする。
   *
   * @throws IllegalArgumentException {@code earlyEmployees}または{@code lateEmployees}の件数が
   *     ちょうど2件でない場合
   */
  public AssignmentResult {
    if (earlyEmployees.size() != 2) {
      throw new IllegalArgumentException("早番は必ずちょうど2名である必要があります");
    }
    if (lateEmployees.size() != 2) {
      throw new IllegalArgumentException("遅番は必ずちょうど2名である必要があります");
    }
    earlyEmployees = List.copyOf(earlyEmployees);
    lateEmployees = List.copyOf(lateEmployees);
    unassignedEmployees = List.copyOf(unassignedEmployees);
  }

  /**
   * 割り当てられた従業員の休憩時刻を返す。
   *
   * <p>早番 → 遅番の順で、各枠は入力順に 1 時間ずつ連続した休憩を割り当てます。
   * 早番 1 人目は 13:00、早番 2 人目は 14:00、遅番 1 人目は 15:00、遅番 2 人目は 16:00 開始です。
   *
   * @return {@link BreakTime} のリスト。早番 2 件、遅番 2 件の計 4 件
   */
  public List<BreakTime> breakTimes() {
    List<BreakTime> breaks = new ArrayList<>();

    breaks.add(new BreakTime(earlyEmployees.get(0), LocalTime.of(13, 0), LocalTime.of(14, 0)));
    breaks.add(new BreakTime(earlyEmployees.get(1), LocalTime.of(14, 0), LocalTime.of(15, 0)));

    breaks.add(new BreakTime(lateEmployees.get(0), LocalTime.of(15, 0), LocalTime.of(16, 0)));
    breaks.add(new BreakTime(lateEmployees.get(1), LocalTime.of(16, 0), LocalTime.of(17, 0)));

    return breaks;
  }
}
