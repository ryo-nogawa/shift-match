package com.example.shiftmatch.domain;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * 従業員の基本情報と曜日ごとの基本シフト・パートの曜日休みを表すレコード。
 *
 * <p>基本シフトの既定の時間帯は 7:30〜18:30 です。曜日休みは雇用区分が「パート」の従業員だけが持てます。曜日休みの曜日は、基本シフトが指定されていても休みを優先し、
 * {@link #baseShifts()} からも除きます。
 */
public record EmployeeProfile(
    String name,
    EmploymentType employmentType,
    Map<DayOfWeek, DailyWish> baseShifts,
    Set<DayOfWeek> offDays) {

  private static final LocalTime DEFAULT_START = LocalTime.of(7, 30);
  private static final LocalTime DEFAULT_END = LocalTime.of(18, 30);

  /**
   * 曜日休みと基本シフトを不変にする。
   *
   * <p>曜日休みは雇用区分が「パート」のときだけ保持し、それ以外は空にします。基本シフトは、曜日休みに含まれる曜日のエントリーを除いて不変コピーにします。
   */
  public EmployeeProfile {
    offDays = employmentType == EmploymentType.PART_TIME ? Set.copyOf(offDays) : Set.of();
    Map<DayOfWeek, DailyWish> filteredBaseShifts = new EnumMap<>(DayOfWeek.class);
    Set<DayOfWeek> effectiveOffDays = offDays;
    for (Map.Entry<DayOfWeek, DailyWish> entry : baseShifts.entrySet()) {
      if (!effectiveOffDays.contains(entry.getKey())) {
        filteredBaseShifts.put(entry.getKey(), entry.getValue());
      }
    }
    baseShifts = Map.copyOf(filteredBaseShifts);
  }

  /**
   * 全曜日の基本シフトを 7:30〜18:30 として作成する便利コンストラクター。
   *
   * @param name 従業員名
   * @param employmentType 雇用区分
   * @param offDays パートの曜日休み
   */
  public EmployeeProfile(String name, EmploymentType employmentType, Set<DayOfWeek> offDays) {
    this(name, employmentType, defaultBaseShifts(), offDays);
  }

  private static Map<DayOfWeek, DailyWish> defaultBaseShifts() {
    Map<DayOfWeek, DailyWish> shifts = new EnumMap<>(DayOfWeek.class);
    for (DayOfWeek day :
        new DayOfWeek[] {
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY
        }) {
      shifts.put(day, new DailyWish(false, DEFAULT_START, DEFAULT_END));
    }
    return shifts;
  }

  /**
   * この従業員プロファイルと指定した曜日から Employee を作成します。
   *
   * <p>曜日休みの曜日は休みにします。それ以外は、その曜日の基本シフトがあればその時間帯で出勤とし、基本シフトがない曜日は 7:30〜18:30 で出勤とします。
   *
   * @param dayOfWeek 曜日
   * @return 新しい Employee インスタンス
   */
  public Employee toEmployee(DayOfWeek dayOfWeek) {
    if (offDays.contains(dayOfWeek)) {
      return Employee.onLeave(name, employmentType);
    }
    DailyWish wish = baseShifts.get(dayOfWeek);
    if (wish == null) {
      return Employee.working(name, employmentType, DEFAULT_START, DEFAULT_END);
    }
    if (wish.off()) {
      return Employee.onLeave(name, employmentType);
    }
    return Employee.working(name, employmentType, wish.start(), wish.end());
  }
}
