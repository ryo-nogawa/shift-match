package com.example.shiftmatch.controller;

import com.example.shiftmatch.controller.MonthlyResultView.CalendarDay;
import com.example.shiftmatch.controller.MonthlyResultView.EmployeeRow;
import com.example.shiftmatch.controller.MonthlyResultView.HolidayCell;
import com.example.shiftmatch.controller.MonthlyResultView.WorkGroup;
import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.MonthEmployee;
import com.example.shiftmatch.domain.MonthlyShiftResult;
import com.example.shiftmatch.domain.ShiftAssignment;
import com.example.shiftmatch.domain.ShiftSlot;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 月間シフトの結果から、結果画面の表示モデルを作ります。
 */
@Component
public class MonthlyResultViewFactory {

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");

  private static final String ON_LEAVE_CELL = "休";

  private static final String NOT_ASSIGNED_CELL = "–";

  private static final String FAILED_CELL = "×";

  /**
   * 表示モデルを作ります。
   *
   * @param result 月間シフトの結果
   * @param employees 従業員（氏名と区分。入力順）
   * @param holidays 対象月の祝日（日付から祝日名）
   * @return 表示モデル
   */
  public MonthlyResultView create(
      MonthlyShiftResult result, List<MonthEmployee> employees, Map<LocalDate, String> holidays) {
    List<DailyShiftResult> dailyResults = result.days();
    int failureCount = 0;
    List<CalendarDay> days = new ArrayList<>();
    for (DailyShiftResult daily : dailyResults) {
      if (daily.assignment().isEmpty()) {
        failureCount++;
      }
      days.add(toCalendarDay(daily, holidays.get(daily.date())));
    }
    List<EmployeeRow> rows = new ArrayList<>();
    for (MonthEmployee employee : employees) {
      rows.add(toEmployeeRow(employee, dailyResults));
    }
    return new MonthlyResultView(
        dailyResults.size(),
        dailyResults.size() - failureCount,
        failureCount,
        days,
        weekdayHolidayCells(holidays),
        rows);
  }

  private static CalendarDay toCalendarDay(DailyShiftResult daily, String holidayName) {
    if (daily.assignment().isEmpty()) {
      return new CalendarDay(
          daily.date(),
          holidayName,
          true,
          daily.failureReason().orElse(null),
          daily.availableCount(),
          List.of());
    }
    Map<String, List<String>> namesByTime = new LinkedHashMap<>();
    for (ShiftAssignment assignment : daily.assignment().get().assignments()) {
      namesByTime
          .computeIfAbsent(workTimeOf(assignment.slot()), key -> new ArrayList<>())
          .add(assignment.employee().name());
    }
    List<WorkGroup> groups = new ArrayList<>();
    for (Map.Entry<String, List<String>> entry : namesByTime.entrySet()) {
      groups.add(new WorkGroup(entry.getKey(), String.join("・", entry.getValue())));
    }
    return new CalendarDay(daily.date(), holidayName, false, null, daily.availableCount(), groups);
  }

  private static EmployeeRow toEmployeeRow(
      MonthEmployee employee, List<DailyShiftResult> dailyResults) {
    String name = employee.name();
    List<String> cells = new ArrayList<>();
    int workDays = 0;
    int totalMinutes = 0;
    for (DailyShiftResult daily : dailyResults) {
      String cell = cellOf(name, daily);
      if (!cell.equals(ON_LEAVE_CELL)
          && !cell.equals(NOT_ASSIGNED_CELL)
          && !cell.equals(FAILED_CELL)) {
        workDays++;
        totalMinutes += actualWorkMinutesOf(name, daily);
      }
      cells.add(cell);
    }
    return new EmployeeRow(
        name, cells, workDays, totalMinutes, weeklyTotalsOf(employee, dailyResults));
  }

  // H-4 の週は対象月の営業日だけで数えるため、結果にある日を月曜始まりでまとめる
  private static List<String> weeklyTotalsOf(
      MonthEmployee employee, List<DailyShiftResult> dailyResults) {
    String name = employee.name();
    // パートかどうかは、成立日の割り当てではなく作成時点の区分で判定する（全日不成立でも出す）
    if (!employee.employmentType().hasWeeklyLimit()) {
      return List.of();
    }
    Map<LocalDate, Integer> minutesByWeek = new LinkedHashMap<>();
    for (DailyShiftResult daily : dailyResults) {
      int minutes = 0;
      for (ShiftAssignment assignment :
          daily.assignment().stream().flatMap(result -> result.assignments().stream()).toList()) {
        if (assignment.employee().name().equals(name)) {
          minutes += assignment.slot().netWorkMinutes();
        }
      }
      minutesByWeek.merge(daily.date().with(DayOfWeek.MONDAY), minutes, (a, b) -> a + b);
    }
    List<String> totals = new ArrayList<>();
    int weekNumber = 1;
    for (int minutes : minutesByWeek.values()) {
      totals.add(
          String.format(
              "%d 週 %d:%02d / %d:00",
              weekNumber++,
              minutes / 60,
              minutes % 60,
              EmploymentType.PART_TIME_WEEKLY_LIMIT_MINUTES / 60));
    }
    return totals;
  }

  private static int actualWorkMinutesOf(String name, DailyShiftResult daily) {
    for (ShiftAssignment shiftAssignment : daily.assignment().get().assignments()) {
      if (shiftAssignment.employee().name().equals(name)) {
        ShiftSlot slot = shiftAssignment.slot();
        return slot.workMinutes() - slot.breakDurationMinutes();
      }
    }
    return 0;
  }

  private static String cellOf(String name, DailyShiftResult daily) {
    if (daily.assignment().isEmpty()) {
      return FAILED_CELL;
    }
    AssignmentResult assignment = daily.assignment().get();
    for (ShiftAssignment shiftAssignment : assignment.assignments()) {
      if (shiftAssignment.employee().name().equals(name)) {
        return workTimeOf(shiftAssignment.slot());
      }
    }
    for (Employee unassigned : assignment.unassignedEmployees()) {
      if (unassigned.name().equals(name) && unassigned.off()) {
        return ON_LEAVE_CELL;
      }
    }
    return NOT_ASSIGNED_CELL;
  }

  private static List<HolidayCell> weekdayHolidayCells(Map<LocalDate, String> holidays) {
    List<HolidayCell> cells = new ArrayList<>();
    for (Map.Entry<LocalDate, String> entry : holidays.entrySet()) {
      DayOfWeek dayOfWeek = entry.getKey().getDayOfWeek();
      if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
        cells.add(new HolidayCell(entry.getKey(), entry.getValue()));
      }
    }
    return cells;
  }

  private static String workTimeOf(ShiftSlot slot) {
    return slot.startTime().format(TIME_FORMATTER) + "–" + slot.endTime().format(TIME_FORMATTER);
  }
}
