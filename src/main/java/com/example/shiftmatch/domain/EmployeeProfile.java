package com.example.shiftmatch.domain;

import java.time.DayOfWeek;
import java.util.Map;
import java.util.Set;

/**
 * 従業員の基本情報と基本シフト・曜日休みを表すレコード。
 *
 * <p>月〜金の基本シフト（勤務可能時間帯または休み）と、パートだけが持てる曜日休みを保持します。
 */
public record EmployeeProfile(
    String name,
    EmploymentType employmentType,
    Map<DayOfWeek, DailyWish> baseShifts,
    Set<DayOfWeek> offDays) {

  /**
   * 基本シフトと曜日休みを不変にする。
   *
   * <p>曜日休みは雇用区分が「パート」のときだけ保持し、それ以外は空にします。
   */
  public EmployeeProfile {
    baseShifts = Map.copyOf(baseShifts);
    offDays = employmentType == EmploymentType.PART_TIME ? Set.copyOf(offDays) : Set.of();
  }

  /**
   * 曜日休みなしで作成します。
   *
   * @param name 従業員名
   * @param employmentType 雇用区分
   * @param baseShifts 基本シフト
   */
  public EmployeeProfile(
      String name, EmploymentType employmentType, Map<DayOfWeek, DailyWish> baseShifts) {
    this(name, employmentType, baseShifts, Set.of());
  }

  /**
   * この従業員プロファイルと指定した日付の希望から Employee を作成します。
   *
   * @param dayOfWeek 曜日
   * @return 新しい Employee インスタンス
   */
  public Employee toEmployee(DayOfWeek dayOfWeek) {
    DailyWish wish = baseShifts.get(dayOfWeek);
    if (offDays.contains(dayOfWeek) || wish == null || wish.off()) {
      return Employee.onLeave(name, employmentType);
    }
    return Employee.working(name, employmentType, wish.start(), wish.end());
  }
}
