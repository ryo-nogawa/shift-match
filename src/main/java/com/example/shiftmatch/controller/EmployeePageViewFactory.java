package com.example.shiftmatch.controller;

import com.example.shiftmatch.controller.EmployeePageView.CellType;
import com.example.shiftmatch.controller.EmployeePageView.DayCell;
import com.example.shiftmatch.controller.EmployeePageView.Week;
import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.MonthEmployee;
import com.example.shiftmatch.domain.MonthlyShiftResult;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
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
    Map<LocalDate, DailyShiftResult> dailyByDate = new HashMap<>();
    for (DailyShiftResult daily : result.days()) {
      dailyByDate.put(daily.date(), daily);
    }
    LocalDate weekStart = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    List<Week> weeks = new ArrayList<>();
    int workDays = 0;
    int totalMinutes = 0;
    int failureCount = 0;
    while (!weekStart.isAfter(month.atEndOfMonth())) {
      List<DayCell> cells = new ArrayList<>();
      int weekMinutes = 0;
      for (int i = 0; i < DAYS_PER_WEEK; i++) {
        LocalDate date = weekStart.plusDays(i);
        DayCell cell = cellOf(month, date, employee.name(), dailyByDate.get(date), holidays);
        cells.add(cell);
        if (cell.type() == CellType.WORK) {
          int minutes =
              MonthlyResultViewFactory.actualWorkMinutesOf(employee.name(), dailyByDate.get(date));
          weekMinutes += minutes;
          workDays++;
          totalMinutes += minutes;
        } else if (cell.type() == CellType.FAILED) {
          failureCount++;
        }
      }
      weeks.add(new Week(cells, weekMinutes));
      weekStart = weekStart.plusWeeks(1);
    }
    return new EmployeePageView(
        month,
        employee.name(),
        employee.employmentType(),
        weeks,
        workDays,
        totalMinutes,
        failureCount);
  }

  private static DayCell cellOf(
      YearMonth month,
      LocalDate date,
      String name,
      DailyShiftResult daily,
      Map<LocalDate, String> holidays) {
    if (!YearMonth.from(date).equals(month)) {
      return new DayCell(null, CellType.BLANK, "");
    }
    if (daily != null) {
      String text = MonthlyResultViewFactory.cellOf(name, daily);
      return new DayCell(date, businessDayTypeOf(text), text);
    }
    String holidayName = holidays.get(date);
    if (holidayName != null) {
      return new DayCell(date, CellType.HOLIDAY, holidayName);
    }
    return new DayCell(date, CellType.WEEKEND, "");
  }

  private static CellType businessDayTypeOf(String text) {
    return switch (text) {
      case MonthlyResultViewFactory.ON_LEAVE_CELL -> CellType.OFF;
      case MonthlyResultViewFactory.NOT_ASSIGNED_CELL -> CellType.NOT_ASSIGNED;
      case MonthlyResultViewFactory.FAILED_CELL -> CellType.FAILED;
      default -> CellType.WORK;
    };
  }
}
