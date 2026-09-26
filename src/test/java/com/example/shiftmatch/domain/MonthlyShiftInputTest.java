package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MonthlyShiftInput")
class MonthlyShiftInputTest {

  @Test
  @DisplayName("従業員リストの変更が MonthlyShiftInput に反映されない")
  void testImmutableEmployees() {
    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    baseShifts.put(DayOfWeek.MONDAY, wish);
    baseShifts.put(DayOfWeek.TUESDAY, wish);
    baseShifts.put(DayOfWeek.WEDNESDAY, wish);
    baseShifts.put(DayOfWeek.THURSDAY, wish);
    baseShifts.put(DayOfWeek.FRIDAY, wish);

    EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);
    List<EmployeeProfile> employees = new ArrayList<>();
    employees.add(profile);

    List<ShiftAdjustment> adjustments = new ArrayList<>();
    MonthlyShiftInput input = new MonthlyShiftInput(YearMonth.of(2024, 9), employees, adjustments);

    // 元のリストを変更
    employees.clear();

    // MonthlyShiftInput の従業員リストは変わらない
    assertEquals(1, input.employees().size());
    assertEquals("Taro", input.employees().get(0).name());
  }

  @Test
  @DisplayName("個別変更リストの変更が MonthlyShiftInput に反映されない")
  void testImmutableAdjustments() {
    List<EmployeeProfile> employees = new ArrayList<>();
    List<ShiftAdjustment> adjustments = new ArrayList<>();
    LocalDate date = LocalDate.of(2024, 9, 2);
    DailyWish wish = new DailyWish(true, null, null);
    adjustments.add(new ShiftAdjustment(date, "Taro", wish));

    YearMonth month = YearMonth.of(2024, 9);
    MonthlyShiftInput input = new MonthlyShiftInput(month, employees, adjustments);

    // 元のリストを変更
    adjustments.clear();

    // MonthlyShiftInput の個別変更リストは変わらない
    assertEquals(1, input.adjustments().size());
  }
}
