package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.MonthlyShiftInput;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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

  private static EmployeeForm employee(String name, String type) {
    EmployeeForm employee = new EmployeeForm();
    employee.setName(name);
    employee.setEmploymentType(type);
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

  private static EmployeeForm employeeWithOffDays(String type, List<Integer> offDays) {
    EmployeeForm employee = employee("山田太郎", type);
    employee.setOffDays(offDays);
    return employee;
  }

  private Set<DayOfWeek> convertedOffDays(String type, List<Integer> offDays) {
    ShiftForm form =
        form("2026-10", List.of(employeeWithOffDays(type, offDays)), new ArrayList<>());
    return converter.toInput(form).employees().get(0).offDays();
  }

  @Nested
  class 曜日休み {

    @Test
    @DisplayName("[F-1] Given: パートの offDays が 0 と 2 のとき, When: 変換すると, Then: 月曜と水曜が曜日休みになる")
    void convertsPartTimeOffDays() {
      Set<DayOfWeek> offDays = convertedOffDays("PART_TIME", List.of(0, 2));

      assertEquals(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), offDays);
    }

    @Test
    @DisplayName("[F-1] Given: 常勤の offDays が 0 のとき, When: 変換すると, Then: 曜日休みは空になる")
    void ignoresOffDaysForFullTime() {
      assertTrue(convertedOffDays("FULL_TIME", List.of(0)).isEmpty());
    }

    @Test
    @DisplayName("[F-1] Given: offDays に範囲外の値と null があるとき, When: 変換すると, Then: それらは無視される")
    void ignoresOutOfRangeOffDays() {
      List<Integer> offDays = new ArrayList<>(Arrays.asList(5, -1, null, 4));

      Set<DayOfWeek> converted = convertedOffDays("PART_TIME", offDays);

      assertEquals(Set.of(DayOfWeek.FRIDAY), converted);
    }
  }

  @Nested
  class 正常系 {

    @Test
    @DisplayName(
        "[F-1] Given: 対象月・氏名・区分が正しいフォームのとき, When: 変換すると," + " Then: 同じ内容の MonthlyShiftInput になる")
    void convertsValidFormToMonthlyShiftInput() {
      ShiftForm form = form("2026-10", List.of(employee("山田太郎", "FULL_TIME")), new ArrayList<>());

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
              List.of(employee("山田太郎", "FULL_TIME")),
              List.of(adjustment("2026-10-20", "山田太郎", true)));

      MonthlyShiftInput input = converter.toInput(form);

      assertEquals(1, input.adjustments().size());
      assertEquals(LocalDate.of(2026, 10, 20), input.adjustments().get(0).date());
      assertEquals("山田太郎", input.adjustments().get(0).employeeName());
      assertTrue(input.adjustments().get(0).wish().off());
    }

    @Test
    @DisplayName("[V-1] Given: 従業員名が空の行があるとき, When: 変換すると," + " Then: その行は除外されずにそのまま渡される")
    void keepsRowWithEmptyEmployeeName() {
      ShiftForm form = form("2026-10", List.of(employee("", "FULL_TIME")), new ArrayList<>());

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
      ShiftForm form = form("invalid", List.of(employee("太郎", "FULL_TIME")), new ArrayList<>());

      MonthlyShiftInput input = converter.toInput(form);

      assertNull(input.month());
    }

    @Test
    @DisplayName("[V-8] Given: 対象月が空のとき, When: 変換すると, Then: 対象月が null になる")
    void returnsNullMonthWhenTargetMonthIsEmpty() {
      ShiftForm form = form("", List.of(employee("太郎", "FULL_TIME")), new ArrayList<>());

      MonthlyShiftInput input = converter.toInput(form);

      assertNull(input.month());
    }

    @Test
    @DisplayName("[V-7] Given: 区分が 3 択以外のとき, When: 変換すると, Then: 区分が null になる")
    void returnsNullEmploymentTypeWhenTypeIsUnknown() {
      ShiftForm form = form("2026-10", List.of(employee("太郎", "INVALID")), new ArrayList<>());

      MonthlyShiftInput input = converter.toInput(form);

      assertNull(input.employees().get(0).employmentType());
    }

    @Test
    @DisplayName("[V-3] Given: 個別変更の開始時刻が HH:mm 形式でないとき, When: 変換すると, Then: 開始時刻が null になる")
    void returnsNullStartWhenAdjustmentTimeIsInvalid() {
      AdjustmentForm adjustment = adjustment("2026-10-20", "太郎", false);
      adjustment.setStart("invalid");
      adjustment.setEnd("18:30");
      ShiftForm form = form("2026-10", List.of(employee("太郎", "FULL_TIME")), List.of(adjustment));

      MonthlyShiftInput input = converter.toInput(form);

      assertNull(input.adjustments().get(0).wish().start());
    }

    @Test
    @DisplayName(
        "[V-9] Given: 個別変更の日付が解析できないとき, When: 変換すると," + " Then: 日付が LocalDate.MIN になり除外されない")
    void usesMinDateWhenAdjustmentDateIsInvalid() {
      ShiftForm form =
          form(
              "2026-10",
              List.of(employee("太郎", "FULL_TIME")),
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
              List.of(employee("太郎", "FULL_TIME")),
              List.of(adjustment("2026-10-20", null, true)));

      MonthlyShiftInput input = converter.toInput(form);

      assertEquals("", input.adjustments().get(0).employeeName());
    }
  }
}
