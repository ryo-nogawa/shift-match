package com.example.shiftmatch.controller;

import com.example.shiftmatch.controller.EmployeePageView.DayCell;
import com.example.shiftmatch.controller.EmployeePageView.Week;
import com.example.shiftmatch.domain.MonthEmployee;
import com.example.shiftmatch.domain.MonthlyShiftResult;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 月間シフトの結果から、従業員の個別ページの表示モデルを作ります。
 */
@Component
public class EmployeePageViewFactory {

  private static final int DAYS_PER_WEEK = 7;

  /**
   * 表示モデルを作ります。
   *
   * @param result 月間シフトの結果
   * @param employee 対象の従業員
   * @param holidays 対象月の祝日（日付から祝日名）
   * @return 表示モデル
   */
  public EmployeePageView create(
      MonthlyShiftResult result, MonthEmployee employee, Map<LocalDate, String> holidays) {
    YearMonth month = result.month();
    LocalDate weekStart = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    List<Week> weeks = new ArrayList<>();
    while (!weekStart.isAfter(month.atEndOfMonth())) {
      List<DayCell> cells = new ArrayList<>();
      for (int i = 0; i < DAYS_PER_WEEK; i++) {
        LocalDate date = weekStart.plusDays(i);
        cells.add(new DayCell(YearMonth.from(date).equals(month) ? date : null));
      }
      weeks.add(new Week(cells));
      weekStart = weekStart.plusWeeks(1);
    }
    return new EmployeePageView(month, employee.name(), employee.employmentType(), weeks);
  }
}
