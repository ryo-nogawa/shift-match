package com.example.shiftmatch.controller;

import com.example.shiftmatch.controller.MonthlyResultView.CalendarDay;
import com.example.shiftmatch.controller.MonthlyResultView.EmployeeRow;
import com.example.shiftmatch.controller.MonthlyResultView.HolidayCell;
import com.example.shiftmatch.controller.MonthlyResultView.WorkGroup;
import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.Employee;
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
   * @param employeeNames 従業員名（入力順）
   * @param holidays 対象月の祝日（日付から祝日名）
   * @return 表示モデル
   */
  public MonthlyResultView create(
      MonthlyShiftResult result, List<String> employeeNames, Map<LocalDate, String> holidays) {
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
    for (String name : employeeNames) {
      rows.add(toEmployeeRow(name, dailyResults));
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
      return new CalendarDay(daily.date(), holidayName, true, daily.availableCount(), List.of());
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
    return new CalendarDay(daily.date(), holidayName, false, daily.availableCount(), groups);
  }

  private static EmployeeRow toEmployeeRow(String name, List<DailyShiftResult> dailyResults) {
    List<String> cells = new ArrayList<>();
    int workDays = 0;
    for (DailyShiftResult daily : dailyResults) {
      String cell = cellOf(name, daily);
      if (!cell.equals(ON_LEAVE_CELL)
          && !cell.equals(NOT_ASSIGNED_CELL)
          && !cell.equals(FAILED_CELL)) {
        workDays++;
      }
      cells.add(cell);
    }
    return new EmployeeRow(name, cells, workDays);
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
