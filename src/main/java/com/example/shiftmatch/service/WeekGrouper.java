package com.example.shiftmatch.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 営業日を週ごとにまとめるクラス。
 *
 * <p>週は月曜始まりです（H-4）。
 */
@Component
public class WeekGrouper {

  /**
   * 営業日を、月曜始まりの週ごとにまとめる。
   *
   * <p>祝日などで欠けた日は含めません。月初・月末で分断される週は、渡された営業日だけで 1 つの週にします。
   *
   * @param businessDays 昇順の営業日
   * @return 週ごとの営業日（日付順）
   */
  public List<List<LocalDate>> group(List<LocalDate> businessDays) {
    List<List<LocalDate>> weeks = new ArrayList<>();
    LocalDate currentWeekStart = null;
    for (LocalDate day : businessDays) {
      LocalDate weekStart = day.with(DayOfWeek.MONDAY);
      if (!weekStart.equals(currentWeekStart)) {
        weeks.add(new ArrayList<>());
        currentWeekStart = weekStart;
      }
      weeks.get(weeks.size() - 1).add(day);
    }
    return weeks;
  }
}
