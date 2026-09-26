package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.MonthlyShiftInput;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MonthlyFormConverter")
class MonthlyFormConverterTest {

  private final MonthlyFormConverter converter = new MonthlyFormConverter();

  @Test
  @DisplayName("[F-1] 正常な入力が MonthlyShiftInput に変換される")
  void testConvertValidInput() {
    ShiftForm form = new ShiftForm();
    form.setTargetMonth("2026-10");

    EmployeeForm employee = new EmployeeForm();
    employee.setName("山田太郎");
    employee.setEmploymentType("FULL_TIME");

    List<DayForm> days = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      DayForm day = new DayForm();
      day.setOff(false);
      day.setStart("07:30");
      day.setEnd("18:30");
      days.add(day);
    }
    employee.setDays(days);
    form.setEmployees(List.of(employee));
    form.setAdjustments(new ArrayList<>());

    MonthlyShiftInput input = converter.toInput(form);

    assertNotNull(input);
    assertEquals(YearMonth.of(2026, 10), input.month());
    assertEquals(1, input.employees().size());
    assertEquals("山田太郎", input.employees().get(0).name());
    assertEquals(EmploymentType.FULL_TIME, input.employees().get(0).employmentType());
  }

  @Test
  @DisplayName("[F-11] 個別変更が ShiftAdjustment に変換される")
  void testConvertAdjustments() {
    ShiftForm form = new ShiftForm();
    form.setTargetMonth("2026-10");

    EmployeeForm employee = new EmployeeForm();
    employee.setName("山田太郎");
    employee.setEmploymentType("FULL_TIME");
    List<DayForm> days = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      DayForm day = new DayForm();
      day.setOff(false);
      day.setStart("07:30");
      day.setEnd("18:30");
      days.add(day);
    }
    employee.setDays(days);
    form.setEmployees(List.of(employee));

    AdjustmentForm adjustment = new AdjustmentForm();
    adjustment.setDate("2026-10-20");
    adjustment.setEmployeeName("山田太郎");
    adjustment.setOff(true);
    form.setAdjustments(List.of(adjustment));

    MonthlyShiftInput input = converter.toInput(form);

    assertEquals(1, input.adjustments().size());
    assertEquals(LocalDate.of(2026, 10, 20), input.adjustments().get(0).date());
    assertEquals("山田太郎", input.adjustments().get(0).employeeName());
    assertTrue(input.adjustments().get(0).wish().off());
  }

  @Test
  @DisplayName("[V-3] 曜日の対応：days[0]＝月曜〜days[4]＝金曜")
  void testDayOfWeekMapping() {
    ShiftForm form = new ShiftForm();
    form.setTargetMonth("2026-10");

    EmployeeForm employee = new EmployeeForm();
    employee.setName("太郎");
    employee.setEmploymentType("FULL_TIME");

    DayForm monday = new DayForm();
    monday.setOff(false);
    monday.setStart("08:00");
    monday.setEnd("17:00");

    List<DayForm> days = new ArrayList<>();
    days.add(monday);

    for (int i = 1; i < 5; i++) {
      DayForm day = new DayForm();
      day.setOff(false);
      day.setStart("07:30");
      day.setEnd("18:30");
      days.add(day);
    }
    employee.setDays(days);
    form.setEmployees(List.of(employee));
    form.setAdjustments(new ArrayList<>());

    MonthlyShiftInput input = converter.toInput(form);

    DailyWish mondayWish = input.employees().get(0).baseShifts().get(DayOfWeek.MONDAY);
    assertEquals(LocalTime.of(8, 0), mondayWish.start());
    assertEquals(LocalTime.of(17, 0), mondayWish.end());

    DailyWish tuesdayWish = input.employees().get(0).baseShifts().get(DayOfWeek.TUESDAY);
    assertEquals(LocalTime.of(7, 30), tuesdayWish.start());
  }

  @Test
  @DisplayName("[V-3] 休みの曜日は off=true かつ start・end が null")
  void testOffDayMapping() {
    ShiftForm form = new ShiftForm();
    form.setTargetMonth("2026-10");

    EmployeeForm employee = new EmployeeForm();
    employee.setName("太郎");
    employee.setEmploymentType("FULL_TIME");

    DayForm monday = new DayForm();
    monday.setOff(true);

    List<DayForm> days = new ArrayList<>();
    days.add(monday);

    for (int i = 1; i < 5; i++) {
      DayForm day = new DayForm();
      day.setOff(false);
      day.setStart("07:30");
      day.setEnd("18:30");
      days.add(day);
    }
    employee.setDays(days);
    form.setEmployees(List.of(employee));
    form.setAdjustments(new ArrayList<>());

    MonthlyShiftInput input = converter.toInput(form);

    DailyWish mondayWish = input.employees().get(0).baseShifts().get(DayOfWeek.MONDAY);
    assertTrue(mondayWish.off());
    assertNull(mondayWish.start());
    assertNull(mondayWish.end());
  }

  @Test
  @DisplayName("[V-3] targetMonth が空・不正形式なら month が null")
  void testInvalidTargetMonth() {
    ShiftForm form = new ShiftForm();
    form.setTargetMonth("invalid");

    EmployeeForm employee = new EmployeeForm();
    employee.setName("太郎");
    employee.setEmploymentType("FULL_TIME");
    List<DayForm> days = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      DayForm day = new DayForm();
      day.setOff(false);
      day.setStart("07:30");
      day.setEnd("18:30");
      days.add(day);
    }
    employee.setDays(days);
    form.setEmployees(List.of(employee));
    form.setAdjustments(new ArrayList<>());

    MonthlyShiftInput input = converter.toInput(form);

    assertNull(input.month());
  }

  @Test
  @DisplayName("[V-3] 区分が 3 択以外なら employmentType が null")
  void testInvalidEmploymentType() {
    ShiftForm form = new ShiftForm();
    form.setTargetMonth("2026-10");

    EmployeeForm employee = new EmployeeForm();
    employee.setName("太郎");
    employee.setEmploymentType("INVALID");
    List<DayForm> days = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      DayForm day = new DayForm();
      day.setOff(false);
      day.setStart("07:30");
      day.setEnd("18:30");
      days.add(day);
    }
    employee.setDays(days);
    form.setEmployees(List.of(employee));
    form.setAdjustments(new ArrayList<>());

    MonthlyShiftInput input = converter.toInput(form);

    assertNull(input.employees().get(0).employmentType());
  }

  @Test
  @DisplayName("[V-3] 時刻が空・不正形式なら null")
  void testInvalidTimeFormat() {
    ShiftForm form = new ShiftForm();
    form.setTargetMonth("2026-10");

    EmployeeForm employee = new EmployeeForm();
    employee.setName("太郎");
    employee.setEmploymentType("FULL_TIME");

    DayForm day1 = new DayForm();
    day1.setOff(false);
    day1.setStart("invalid");
    day1.setEnd("18:30");

    List<DayForm> days = new ArrayList<>();
    days.add(day1);

    for (int i = 1; i < 5; i++) {
      DayForm day = new DayForm();
      day.setOff(false);
      day.setStart("07:30");
      day.setEnd("18:30");
      days.add(day);
    }
    employee.setDays(days);
    form.setEmployees(List.of(employee));
    form.setAdjustments(new ArrayList<>());

    MonthlyShiftInput input = converter.toInput(form);

    DailyWish wish = input.employees().get(0).baseShifts().get(DayOfWeek.MONDAY);
    assertNull(wish.start());
  }

  @Test
  @DisplayName("[V-3] days が 5 件未満のとき不足する曜日が DailyWish(false, null, null)")
  void testInsufficientDays() {
    ShiftForm form = new ShiftForm();
    form.setTargetMonth("2026-10");

    EmployeeForm employee = new EmployeeForm();
    employee.setName("太郎");
    employee.setEmploymentType("FULL_TIME");

    DayForm day1 = new DayForm();
    day1.setOff(false);
    day1.setStart("07:30");
    day1.setEnd("18:30");

    List<DayForm> days = new ArrayList<>();
    days.add(day1);
    employee.setDays(days);
    form.setEmployees(List.of(employee));
    form.setAdjustments(new ArrayList<>());

    MonthlyShiftInput input = converter.toInput(form);

    DailyWish mondayWish = input.employees().get(0).baseShifts().get(DayOfWeek.MONDAY);
    assertFalse(mondayWish.off());
    assertEquals(LocalTime.of(7, 30), mondayWish.start());

    DailyWish tuesdayWish = input.employees().get(0).baseShifts().get(DayOfWeek.TUESDAY);
    assertFalse(tuesdayWish.off());
    assertNull(tuesdayWish.start());
    assertNull(tuesdayWish.end());
  }

  @Test
  @DisplayName("[V-8] 個別変更で日付が解析できないときは LocalDate.MIN")
  void testInvalidAdjustmentDate() {
    ShiftForm form = new ShiftForm();
    form.setTargetMonth("2026-10");

    EmployeeForm employee = new EmployeeForm();
    employee.setName("太郎");
    employee.setEmploymentType("FULL_TIME");
    List<DayForm> days = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      DayForm day = new DayForm();
      day.setOff(false);
      day.setStart("07:30");
      day.setEnd("18:30");
      days.add(day);
    }
    employee.setDays(days);
    form.setEmployees(List.of(employee));

    AdjustmentForm adjustment = new AdjustmentForm();
    adjustment.setDate("invalid-date");
    adjustment.setEmployeeName("太郎");
    adjustment.setOff(true);
    form.setAdjustments(List.of(adjustment));

    MonthlyShiftInput input = converter.toInput(form);

    assertEquals(LocalDate.MIN, input.adjustments().get(0).date());
  }

  @Test
  @DisplayName("[V-1] 従業員名が空の行も除外されずに渡される")
  void testEmptyEmployeeNameNotExcluded() {
    ShiftForm form = new ShiftForm();
    form.setTargetMonth("2026-10");

    EmployeeForm employee = new EmployeeForm();
    employee.setName("");
    employee.setEmploymentType("FULL_TIME");
    List<DayForm> days = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      DayForm day = new DayForm();
      day.setOff(false);
      day.setStart("07:30");
      day.setEnd("18:30");
      days.add(day);
    }
    employee.setDays(days);
    form.setEmployees(List.of(employee));
    form.setAdjustments(new ArrayList<>());

    MonthlyShiftInput input = converter.toInput(form);

    assertEquals(1, input.employees().size());
    assertEquals("", input.employees().get(0).name());
  }

  @Test
  @DisplayName("[V-11] 個別変更の employeeName が null のときは空文字になる")
  void testNullAdjustmentEmployeeName() {
    ShiftForm form = new ShiftForm();
    form.setTargetMonth("2026-10");

    EmployeeForm employee = new EmployeeForm();
    employee.setName("太郎");
    employee.setEmploymentType("FULL_TIME");
    List<DayForm> days = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      DayForm day = new DayForm();
      day.setOff(false);
      day.setStart("07:30");
      day.setEnd("18:30");
      days.add(day);
    }
    employee.setDays(days);
    form.setEmployees(List.of(employee));

    AdjustmentForm adjustment = new AdjustmentForm();
    adjustment.setDate("2026-10-20");
    adjustment.setEmployeeName(null);
    adjustment.setOff(true);
    form.setAdjustments(List.of(adjustment));

    MonthlyShiftInput input = converter.toInput(form);

    assertEquals("", input.adjustments().get(0).employeeName());
  }
}
