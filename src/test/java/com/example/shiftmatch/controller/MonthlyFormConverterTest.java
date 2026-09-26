package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MonthlyFormConverterTest {

  private MonthlyFormConverter converter;

  @BeforeEach
  void setUp() {
    converter = new MonthlyFormConverter();
  }

  private static DayForm day(boolean off, String start, String end) {
    DayForm day = new DayForm();
    day.setOff(off);
    day.setStart(start);
    day.setEnd(end);
    return day;
  }

  private static List<DayForm> fullWeek() {
    List<DayForm> days = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      days.add(day(false, "07:30", "18:30"));
    }
    return days;
  }

  private static EmployeeForm employee(String name, String type, List<DayForm> days) {
    EmployeeForm employee = new EmployeeForm();
    employee.setName(name);
    employee.setEmploymentType(type);
    employee.setDays(days);
    return employee;
  }

  private static ShiftForm form(
      String month, List<EmployeeForm> employees, List<AdjustmentForm> adjustments) {
    ShiftForm form = new ShiftForm();
    form.setTargetMonth(month);
    form.setEmployees(employees);
    form.setAdjustments(adjustments);
    return form;
  }

  private static AdjustmentForm adjustment(String date, String employeeName, boolean off) {
    AdjustmentForm adjustment = new AdjustmentForm();
    adjustment.setDate(date);
    adjustment.setEmployeeName(employeeName);
    adjustment.setOff(off);
    return adjustment;
  }

  @Nested
  class 正常系 {

    @Test
    @DisplayName(
        "[F-1] Given: 対象月・氏名・区分・基本シフトが正しいフォームのとき, When: 変換すると,"
            + " Then: 同じ内容の MonthlyShiftInput になる")
    void convertsValidFormToMonthlyShiftInput() {
      ShiftForm form =
          form("2026-10", List.of(employee("山田太郎", "FULL_TIME", fullWeek())), new ArrayList<>());

      MonthlyShiftInput input = converter.toInput(form);

      assertEquals(YearMonth.of(2026, 10), input.month());
      assertEquals(1, input.employees().size());
      assertEquals("山田太郎", input.employees().get(0).name());
      assertEquals(EmploymentType.FULL_TIME, input.employees().get(0).employmentType());
    }

    @Test
    @DisplayName(
        "[F-11] Given: 個別変更を含むフォームのとき, When: 変換すると," + " Then: 日付・従業員名・休みが ShiftAdjustment になる")
    void convertsAdjustmentToShiftAdjustment() {
      ShiftForm form =
          form(
              "2026-10",
              List.of(employee("山田太郎", "FULL_TIME", fullWeek())),
              List.of(adjustment("2026-10-20", "山田太郎", true)));

      MonthlyShiftInput input = converter.toInput(form);

      assertEquals(1, input.adjustments().size());
      assertEquals(LocalDate.of(2026, 10, 20), input.adjustments().get(0).date());
      assertEquals("山田太郎", input.adjustments().get(0).employeeName());
      assertTrue(input.adjustments().get(0).wish().off());
    }

    @Test
    @DisplayName(
        "[F-1] Given: 月曜だけ 08:00〜17:00 の基本シフトのとき, When: 変換すると,"
            + " Then: days[0] が月曜、days[1] が火曜に対応する")
    void mapsDayIndexToDayOfWeek() {
      List<DayForm> days = fullWeek();
      days.set(0, day(false, "08:00", "17:00"));
      ShiftForm form =
          form("2026-10", List.of(employee("太郎", "FULL_TIME", days)), new ArrayList<>());

      MonthlyShiftInput input = converter.toInput(form);

      DailyWish monday = input.employees().get(0).baseShifts().get(DayOfWeek.MONDAY);
      assertEquals(LocalTime.of(8, 0), monday.start());
      assertEquals(LocalTime.of(17, 0), monday.end());
      DailyWish tuesday = input.employees().get(0).baseShifts().get(DayOfWeek.TUESDAY);
      assertEquals(LocalTime.of(7, 30), tuesday.start());
    }

    @Test
    @DisplayName(
        "[F-1] Given: 月曜が休みの基本シフトのとき, When: 変換すると," + " Then: 月曜は off=true で開始・終了が null になる")
    void mapsOffDayToOffWishWithoutTimes() {
      List<DayForm> days = fullWeek();
      days.set(0, day(true, null, null));
      ShiftForm form =
          form("2026-10", List.of(employee("太郎", "FULL_TIME", days)), new ArrayList<>());

      MonthlyShiftInput input = converter.toInput(form);

      DailyWish monday = input.employees().get(0).baseShifts().get(DayOfWeek.MONDAY);
      assertTrue(monday.off());
      assertNull(monday.start());
      assertNull(monday.end());
    }

    @Test
    @DisplayName("[V-1] Given: 従業員名が空の行があるとき, When: 変換すると," + " Then: その行は除外されずにそのまま渡される")
    void keepsRowWithEmptyEmployeeName() {
      ShiftForm form =
          form("2026-10", List.of(employee("", "FULL_TIME", fullWeek())), new ArrayList<>());

      MonthlyShiftInput input = converter.toInput(form);

      assertEquals(1, input.employees().size());
      assertEquals("", input.employees().get(0).name());
    }
  }

  @Nested
  class 異常系 {

    @Test
    @DisplayName("[V-8] Given: 対象月が YYYY-MM 形式でないとき, When: 変換すると, Then: 対象月が null になる")
    void returnsNullMonthWhenTargetMonthIsInvalid() {
      ShiftForm form =
          form("invalid", List.of(employee("太郎", "FULL_TIME", fullWeek())), new ArrayList<>());

      MonthlyShiftInput input = converter.toInput(form);

      assertNull(input.month());
    }

    @Test
    @DisplayName("[V-8] Given: 対象月が空のとき, When: 変換すると, Then: 対象月が null になる")
    void returnsNullMonthWhenTargetMonthIsEmpty() {
      ShiftForm form =
          form("", List.of(employee("太郎", "FULL_TIME", fullWeek())), new ArrayList<>());

      MonthlyShiftInput input = converter.toInput(form);

      assertNull(input.month());
    }

    @Test
    @DisplayName("[V-7] Given: 区分が 3 択以外のとき, When: 変換すると, Then: 区分が null になる")
    void returnsNullEmploymentTypeWhenTypeIsUnknown() {
      ShiftForm form =
          form("2026-10", List.of(employee("太郎", "INVALID", fullWeek())), new ArrayList<>());

      MonthlyShiftInput input = converter.toInput(form);

      assertNull(input.employees().get(0).employmentType());
    }

    @Test
    @DisplayName("[V-3] Given: 開始時刻が HH:mm 形式でないとき, When: 変換すると, Then: 開始時刻が null になる")
    void returnsNullStartWhenTimeIsInvalid() {
      List<DayForm> days = fullWeek();
      days.set(0, day(false, "invalid", "18:30"));
      ShiftForm form =
          form("2026-10", List.of(employee("太郎", "FULL_TIME", days)), new ArrayList<>());

      MonthlyShiftInput input = converter.toInput(form);

      assertNull(input.employees().get(0).baseShifts().get(DayOfWeek.MONDAY).start());
    }

    @Test
    @DisplayName(
        "[V-3] Given: 基本シフトが月曜の 1 件しかないとき, When: 変換すると," + " Then: 不足する曜日は休みなし・開始終了 null になる")
    void fillsMissingDaysWithEmptyWish() {
      List<DayForm> days = new ArrayList<>();
      days.add(day(false, "07:30", "18:30"));
      ShiftForm form =
          form("2026-10", List.of(employee("太郎", "FULL_TIME", days)), new ArrayList<>());

      MonthlyShiftInput input = converter.toInput(form);

      DailyWish monday = input.employees().get(0).baseShifts().get(DayOfWeek.MONDAY);
      assertFalse(monday.off());
      assertEquals(LocalTime.of(7, 30), monday.start());
      DailyWish tuesday = input.employees().get(0).baseShifts().get(DayOfWeek.TUESDAY);
      assertFalse(tuesday.off());
      assertNull(tuesday.start());
      assertNull(tuesday.end());
    }

    @Test
    @DisplayName(
        "[V-9] Given: 個別変更の日付が解析できないとき, When: 変換すると," + " Then: 日付が LocalDate.MIN になり除外されない")
    void usesMinDateWhenAdjustmentDateIsInvalid() {
      ShiftForm form =
          form(
              "2026-10",
              List.of(employee("太郎", "FULL_TIME", fullWeek())),
              List.of(adjustment("invalid-date", "太郎", true)));

      MonthlyShiftInput input = converter.toInput(form);

      assertEquals(1, input.adjustments().size());
      assertEquals(LocalDate.MIN, input.adjustments().get(0).date());
    }

    @Test
    @DisplayName("[F-11] Given: 個別変更の従業員名が null のとき, When: 変換すると, Then: 従業員名が空文字になる")
    void usesEmptyNameWhenAdjustmentEmployeeNameIsNull() {
      ShiftForm form =
          form(
              "2026-10",
              List.of(employee("太郎", "FULL_TIME", fullWeek())),
              List.of(adjustment("2026-10-20", null, true)));

      MonthlyShiftInput input = converter.toInput(form);

      assertEquals("", input.adjustments().get(0).employeeName());
    }
  }
}
