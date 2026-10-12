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

  static final String ON_LEAVE_CELL = "休";

  static final String NOT_ASSIGNED_CELL = "–";

  static final String FAILED_CELL = "×";

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
      rows.add(toEmployeeRow(employee.name(), employee.employmentType(), dailyResults));
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
          daily.availableCount(),
          List.of(),
          daily.failureReason());
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
    return new CalendarDay(daily.date(), holidayName, false, daily.availableCount(), groups, null);
  }

  private static EmployeeRow toEmployeeRow(
      String name, EmploymentType employmentType, List<DailyShiftResult> dailyResults) {
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
    return new EmployeeRow(name, employmentType, cells, workDays, totalMinutes);
  }

  /**
   * 従業員がその日に割り当てられた枠の実労働時間（分）を返します。
   *
   * @param name 従業員名
   * @param daily 成立した日の結果
   * @return 実労働時間（分）。割り当てがなければ 0
   */
  static int actualWorkMinutesOf(String name, DailyShiftResult daily) {
    for (ShiftAssignment shiftAssignment : daily.assignment().get().assignments()) {
      if (shiftAssignment.employee().name().equals(name)) {
        return shiftAssignment.slot().actualWorkMinutes();
      }
    }
    return 0;
  }

  /**
   * 従業員別表示のセルの文字列を返します。
   *
   * @param name 従業員名
   * @param daily その日の結果
   * @return 勤務時間、{@code 休}、{@code –}、{@code ×} のいずれか
   */
  static String cellOf(String name, DailyShiftResult daily) {
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

  /**
   * 枠の勤務時間を {@code 7:30–14:30} 形式で返します。
   *
   * @param slot 枠
   * @return 勤務時間の文字列
   */
  static String workTimeOf(ShiftSlot slot) {
    return slot.startTime().format(TIME_FORMATTER) + "–" + slot.endTime().format(TIME_FORMATTER);
  }
}
