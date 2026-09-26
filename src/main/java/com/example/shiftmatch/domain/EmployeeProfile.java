package com.example.shiftmatch.domain;

import java.time.DayOfWeek;
import java.util.Map;

/**
 * 従業員の基本情報と基本シフトを表すレコード。
 *
 * <p>月〜金の基本シフト（勤務可能時間帯または休み）を保持します。
 */
public record EmployeeProfile(
    String name, EmploymentType employmentType, Map<DayOfWeek, DailyWish> baseShifts) {

  /**
   * 基本シフトのマップを不変にする。
   *
   * <p>呼び出し側がマップを変更しても、レコードの内容に反映されないようにします。
   */
  public EmployeeProfile {
    baseShifts = Map.copyOf(baseShifts);
  }

  /**
   * この従業員プロファイルと指定した日付の希望から Employee を作成します。
   *
   * @param dayOfWeek 曜日
   * @return 新しい Employee インスタンス
   */
  public Employee toEmployee(DayOfWeek dayOfWeek) {
    DailyWish wish = baseShifts.get(dayOfWeek);
    if (wish == null || wish.off()) {
      return Employee.onLeave(name, employmentType);
    }
    return Employee.working(name, employmentType, wish.start(), wish.end());
  }
}
