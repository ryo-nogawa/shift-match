package com.example.shiftmatch.controller;

import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.ShiftAdjustment;
import com.example.shiftmatch.service.SavedInput;
import java.time.DayOfWeek;
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

  private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  /** 画面 1 に表示するデモ用の従業員（氏名・区分・曜日休み。0＝月〜4＝金）。 */
  private record DemoEmployee(String name, String employmentType, List<Integer> offDays) {}

  private static final List<DemoEmployee> DEMO_EMPLOYEES =
      List.of(
          new DemoEmployee("佐藤太郎", "FULL_TIME", List.of()),
          new DemoEmployee("鈴木花子", "FULL_TIME", List.of()),
          new DemoEmployee("高橋健一", "FULL_TIME", List.of()),
          new DemoEmployee("田中美咲", "FULL_TIME", List.of()),
          new DemoEmployee("伊藤大輔", "FULL_TIME", List.of()),
          new DemoEmployee("渡辺陽子", "PART_TIME", List.of(0, 2)),
          new DemoEmployee("山本翔太", "PART_TIME", List.of(1, 3)),
          new DemoEmployee("中村由美", "PART_TIME", List.of(4)),
          new DemoEmployee("小林誠", "MANAGER", List.of()),
          new DemoEmployee("加藤恵", "FULL_TIME", List.of()),
          new DemoEmployee("吉田拓也", "PART_TIME", List.of(0, 3)),
          new DemoEmployee("山田彩香", "MANAGER", List.of()));

  /**
   * 保存済みの入力から画面 1 のフォームを作ります。
   *
   * <p>保存済みの従業員が 1 名以上あるときは、先頭の行に復元し、12 行になるまで空の行で埋めます。保存済みの従業員が 0
   * 件のときは、デモ用の従業員 12 名を表示します（保存はしません）。
   *
   * @param saved 保存済みの入力
   * @param defaultMonth 保存済みの最後の対象月がないときに使う対象月
   * @return 画面 1 のフォーム
   */
  public ShiftForm toForm(SavedInput saved, YearMonth defaultMonth) {
    ShiftForm form = new ShiftForm();
    form.setTargetMonth(saved.lastTargetMonth().orElse(defaultMonth).format(MONTH_FORMATTER));

    if (saved.employees().isEmpty()) {
      form.setEmployees(demoEmployeeForms());
    } else {
      form.setEmployees(restoredEmployeeForms(saved));
    }

    List<AdjustmentForm> adjustments = new ArrayList<>();
    for (ShiftAdjustment adjustment : saved.adjustments()) {
      adjustments.add(toAdjustmentForm(adjustment));
    }
    form.setAdjustments(adjustments);
    return form;
  }

  private List<EmployeeForm> restoredEmployeeForms(SavedInput saved) {
    List<EmployeeForm> employees = new ArrayList<>();
    for (EmployeeProfile profile : saved.employees()) {
      employees.add(toEmployeeForm(profile));
    }
    while (employees.size() < DEFAULT_EMPLOYEE_COUNT) {
      employees.add(emptyEmployeeForm());
    }
    return employees;
  }

  private List<EmployeeForm> demoEmployeeForms() {
    List<EmployeeForm> employees = new ArrayList<>();
    for (DemoEmployee demo : DEMO_EMPLOYEES) {
      EmployeeForm employee = new EmployeeForm();
      employee.setName(demo.name());
      employee.setEmploymentType(demo.employmentType());
      employee.setOffDays(new ArrayList<>(demo.offDays()));
      employees.add(employee);
    }
    return employees;
  }

  private EmployeeForm toEmployeeForm(EmployeeProfile profile) {
    EmployeeForm employee = new EmployeeForm();
    employee.setName(profile.name());
    employee.setEmploymentType(profile.employmentType().name());
    List<Integer> offDays = new ArrayList<>();
    for (DayOfWeek day : profile.offDays()) {
      offDays.add(day.ordinal());
    }
    offDays.sort(null);
    employee.setOffDays(offDays);
    return employee;
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
    return employee;
  }
}
