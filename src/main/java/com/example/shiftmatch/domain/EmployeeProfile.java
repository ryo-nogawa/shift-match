package com.example.shiftmatch.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

/**
 * 従業員の基本情報とパートの曜日休みを表すレコード。
 *
 * <p>休みでない日の既定の時間帯は 7:30〜18:30 です。曜日休みは雇用区分が「パート」の従業員だけが持てます。
 */
public record EmployeeProfile(String name, EmploymentType employmentType, Set<DayOfWeek> offDays) {

  private static final LocalTime DEFAULT_START = LocalTime.of(7, 30);
  private static final LocalTime DEFAULT_END = LocalTime.of(18, 30);

  /**
   * 曜日休みを不変にする。
   *
   * <p>曜日休みは雇用区分が「パート」のときだけ保持し、それ以外は空にします。
   */
  public EmployeeProfile {
    offDays = employmentType == EmploymentType.PART_TIME ? Set.copyOf(offDays) : Set.of();
  }

  /**
   * この従業員プロファイルと指定した曜日から Employee を作成します。
   *
   * <p>曜日休みの曜日は休み、それ以外は既定の時間帯（7:30〜18:30）で出勤とします。
   *
   * @param dayOfWeek 曜日
   * @return 新しい Employee インスタンス
   */
  public Employee toEmployee(DayOfWeek dayOfWeek) {
    if (offDays.contains(dayOfWeek)) {
      return Employee.onLeave(name, employmentType);
    }
    return Employee.working(name, employmentType, DEFAULT_START, DEFAULT_END);
  }
}
