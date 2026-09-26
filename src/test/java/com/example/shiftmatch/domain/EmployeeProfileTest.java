package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EmployeeProfile")
class EmployeeProfileTest {

  @Test
  @DisplayName("基本シフトのマップの変更が EmployeeProfile に反映されない")
  void testImmutableBaseShifts() {
    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    baseShifts.put(DayOfWeek.MONDAY, new DailyWish(false, start, end));
    baseShifts.put(DayOfWeek.TUESDAY, new DailyWish(false, start, end));
    baseShifts.put(DayOfWeek.WEDNESDAY, new DailyWish(false, start, end));
    baseShifts.put(DayOfWeek.THURSDAY, new DailyWish(false, start, end));
    baseShifts.put(DayOfWeek.FRIDAY, new DailyWish(false, start, end));

    EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);

    // 元のマップを変更
    baseShifts.put(DayOfWeek.MONDAY, new DailyWish(true, null, null));

    // EmployeeProfile の基本シフトは変わらない
    DailyWish mondayShift = profile.baseShifts().get(DayOfWeek.MONDAY);
    assertNotNull(mondayShift);
    assertFalse(mondayShift.off());
    assertEquals(start, mondayShift.start());
    assertEquals(end, mondayShift.end());
  }

  @Test
  @DisplayName("基本シフトから Employee を作成できる")
  void testToEmployee() {
    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    baseShifts.put(DayOfWeek.MONDAY, wish);
    baseShifts.put(DayOfWeek.TUESDAY, wish);
    baseShifts.put(DayOfWeek.WEDNESDAY, wish);
    baseShifts.put(DayOfWeek.THURSDAY, wish);
    baseShifts.put(DayOfWeek.FRIDAY, wish);

    EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.PART_TIME, baseShifts);
    Employee employee = profile.toEmployee(DayOfWeek.MONDAY);

    assertEquals("Taro", employee.name());
    assertEquals(EmploymentType.PART_TIME, employee.employmentType());
    assertFalse(employee.off());
    assertEquals(start, employee.start());
    assertEquals(end, employee.end());
  }

  @Test
  @DisplayName("基本シフトから Employee を作成するときに休みが反映される")
  void testToEmployeeWithLeave() {
    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    baseShifts.put(DayOfWeek.MONDAY, new DailyWish(true, null, null));
    baseShifts.put(DayOfWeek.TUESDAY, new DailyWish(false, start, end));
    baseShifts.put(DayOfWeek.WEDNESDAY, new DailyWish(false, start, end));
    baseShifts.put(DayOfWeek.THURSDAY, new DailyWish(false, start, end));
    baseShifts.put(DayOfWeek.FRIDAY, new DailyWish(false, start, end));

    EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);
    Employee employee = profile.toEmployee(DayOfWeek.MONDAY);

    assertEquals("Taro", employee.name());
    assertEquals(EmploymentType.FULL_TIME, employee.employmentType());
    assertTrue(employee.off());
    assertNull(employee.start());
    assertNull(employee.end());
  }
}
