package com.example.shiftmatch.controller;

import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.ShiftAdjustment;
import com.example.shiftmatch.service.SavedInput;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 保存済みの入力を画面 1 のフォームに変換します。
 */
@Component
public class SavedInputFormConverter {

  private static final int DEFAULT_EMPLOYEE_COUNT = 12;

  private static final String DEFAULT_START_TIME = "07:30";

  private static final String DEFAULT_END_TIME = "18:30";

  private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  private static final List<DayOfWeek> WEEKDAYS =
      List.of(
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY);

  /**
   * 保存済みの入力から画面 1 のフォームを作ります。
   *
   * <p>保存済みの従業員を先頭の行に復元し、12 行になるまで空の行で埋めます。
   *
   * @param saved 保存済みの入力
   * @param defaultMonth 保存済みの最後の対象月がないときに使う対象月
   * @return 画面 1 のフォーム
   */
  public ShiftForm toForm(SavedInput saved, YearMonth defaultMonth) {
    ShiftForm form = new ShiftForm();
    form.setTargetMonth(saved.lastTargetMonth().orElse(defaultMonth).format(MONTH_FORMATTER));

    List<EmployeeForm> employees = new ArrayList<>();
    for (EmployeeProfile profile : saved.employees()) {
      employees.add(toEmployeeForm(profile));
    }
    while (employees.size() < DEFAULT_EMPLOYEE_COUNT) {
      employees.add(emptyEmployeeForm());
    }
    form.setEmployees(employees);

    List<AdjustmentForm> adjustments = new ArrayList<>();
    for (ShiftAdjustment adjustment : saved.adjustments()) {
      adjustments.add(toAdjustmentForm(adjustment));
    }
    form.setAdjustments(adjustments);
    return form;
  }

  private EmployeeForm toEmployeeForm(EmployeeProfile profile) {
    EmployeeForm employee = new EmployeeForm();
    employee.setName(profile.name());
    employee.setEmploymentType(profile.employmentType().name());
    List<DayForm> days = new ArrayList<>();
    for (DayOfWeek dayOfWeek : WEEKDAYS) {
      days.add(toDayForm(profile.baseShifts().get(dayOfWeek)));
    }
    employee.setDays(days);
    return employee;
  }

  private DayForm toDayForm(DailyWish wish) {
    if (wish == null || wish.off()) {
      return emptyDayForm();
    }
    DayForm day = new DayForm();
    day.setStart(formatOrDefault(wish.start(), DEFAULT_START_TIME));
    day.setEnd(formatOrDefault(wish.end(), DEFAULT_END_TIME));
    return day;
  }

  private AdjustmentForm toAdjustmentForm(ShiftAdjustment adjustment) {
    AdjustmentForm form = new AdjustmentForm();
    form.setDate(adjustment.date().toString());
    form.setEmployeeName(adjustment.employeeName());
    DailyWish wish = adjustment.wish();
    form.setOff(wish.off());
    if (!wish.off()) {
      form.setStart(wish.start().format(TIME_FORMATTER));
      form.setEnd(wish.end().format(TIME_FORMATTER));
    }
    return form;
  }

  private EmployeeForm emptyEmployeeForm() {
    EmployeeForm employee = new EmployeeForm();
    employee.setName("");
    employee.setEmploymentType("FULL_TIME");
    List<DayForm> days = new ArrayList<>();
    for (int i = 0; i < WEEKDAYS.size(); i++) {
      days.add(emptyDayForm());
    }
    employee.setDays(days);
    return employee;
  }

  private DayForm emptyDayForm() {
    DayForm day = new DayForm();
    day.setStart(DEFAULT_START_TIME);
    day.setEnd(DEFAULT_END_TIME);
    return day;
  }

  private static String formatOrDefault(LocalTime time, String defaultValue) {
    return time == null ? defaultValue : time.format(TIME_FORMATTER);
  }
}
