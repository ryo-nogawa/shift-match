package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.ShiftAdjustment;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("[F-11] 希望の優先順位")
class WishResolverTest {

  @Test
  @DisplayName("個別変更がなければ曜日の基本シフトを使う")
  void testUseBaseShiftWhenNoAdjustment() {
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

    WishResolver resolver = new WishResolver();
    LocalDate tuesdayDate = LocalDate.of(2024, 9, 3); // Tuesday
    DailyWish resolved = resolver.resolve(profile, tuesdayDate, new ArrayList<>());

    assertFalse(resolved.off());
    assertEquals(start, resolved.start());
    assertEquals(end, resolved.end());
  }

  @Test
  @DisplayName("個別変更があれば基本シフトより優先する")
  void testAdjustmentTakesPrecedence() {
    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime baseStart = LocalTime.of(9, 0);
    LocalTime baseEnd = LocalTime.of(18, 0);
    DailyWish baseWish = new DailyWish(false, baseStart, baseEnd);
    baseShifts.put(DayOfWeek.MONDAY, baseWish);
    baseShifts.put(DayOfWeek.TUESDAY, baseWish);
    baseShifts.put(DayOfWeek.WEDNESDAY, baseWish);
    baseShifts.put(DayOfWeek.THURSDAY, baseWish);
    baseShifts.put(DayOfWeek.FRIDAY, baseWish);

    EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);

    LocalDate tuesdayDate = LocalDate.of(2024, 9, 3); // Tuesday
    LocalTime adjustStart = LocalTime.of(10, 0);
    LocalTime adjustEnd = LocalTime.of(17, 0);
    DailyWish adjustWish = new DailyWish(false, adjustStart, adjustEnd);
    ShiftAdjustment adjustment = new ShiftAdjustment(tuesdayDate, "Taro", adjustWish);
    List<ShiftAdjustment> adjustments = new ArrayList<>();
    adjustments.add(adjustment);

    WishResolver resolver = new WishResolver();
    DailyWish resolved = resolver.resolve(profile, tuesdayDate, adjustments);

    assertFalse(resolved.off());
    assertEquals(adjustStart, resolved.start());
    assertEquals(adjustEnd, resolved.end());
  }

  @Test
  @DisplayName("個別変更は日付が違う日には効かない")
  void testAdjustmentSpecificToDate() {
    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime baseStart = LocalTime.of(9, 0);
    LocalTime baseEnd = LocalTime.of(18, 0);
    DailyWish baseWish = new DailyWish(false, baseStart, baseEnd);
    baseShifts.put(DayOfWeek.MONDAY, baseWish);
    baseShifts.put(DayOfWeek.TUESDAY, baseWish);
    baseShifts.put(DayOfWeek.WEDNESDAY, baseWish);
    baseShifts.put(DayOfWeek.THURSDAY, baseWish);
    baseShifts.put(DayOfWeek.FRIDAY, baseWish);

    EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);

    LocalDate mondayDate = LocalDate.of(2024, 9, 2);
    LocalDate tuesdayDate = LocalDate.of(2024, 9, 3);
    LocalTime adjustStart = LocalTime.of(10, 0);
    LocalTime adjustEnd = LocalTime.of(17, 0);
    DailyWish adjustWish = new DailyWish(false, adjustStart, adjustEnd);
    ShiftAdjustment adjustment = new ShiftAdjustment(mondayDate, "Taro", adjustWish);
    List<ShiftAdjustment> adjustments = new ArrayList<>();
    adjustments.add(adjustment);

    WishResolver resolver = new WishResolver();
    // Tuesday の希望を解決（調整は Monday 用）
    DailyWish resolved = resolver.resolve(profile, tuesdayDate, adjustments);

    // 基本シフトが使われるはず
    assertFalse(resolved.off());
    assertEquals(baseStart, resolved.start());
    assertEquals(baseEnd, resolved.end());
  }

  @Test
  @DisplayName("氏名が一致しない個別変更は無視される")
  void testAdjustmentWithMismatchedName() {
    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime baseStart = LocalTime.of(9, 0);
    LocalTime baseEnd = LocalTime.of(18, 0);
    DailyWish baseWish = new DailyWish(false, baseStart, baseEnd);
    baseShifts.put(DayOfWeek.MONDAY, baseWish);
    baseShifts.put(DayOfWeek.TUESDAY, baseWish);
    baseShifts.put(DayOfWeek.WEDNESDAY, baseWish);
    baseShifts.put(DayOfWeek.THURSDAY, baseWish);
    baseShifts.put(DayOfWeek.FRIDAY, baseWish);

    EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);

    LocalDate tuesdayDate = LocalDate.of(2024, 9, 3);
    LocalTime adjustStart = LocalTime.of(10, 0);
    LocalTime adjustEnd = LocalTime.of(17, 0);
    DailyWish adjustWish = new DailyWish(false, adjustStart, adjustEnd);
    ShiftAdjustment adjustment = new ShiftAdjustment(tuesdayDate, "Hanako", adjustWish);
    List<ShiftAdjustment> adjustments = new ArrayList<>();
    adjustments.add(adjustment);

    WishResolver resolver = new WishResolver();
    DailyWish resolved = resolver.resolve(profile, tuesdayDate, adjustments);

    // 氏名が一致しないので基本シフトが使われるはず
    assertFalse(resolved.off());
    assertEquals(baseStart, resolved.start());
    assertEquals(baseEnd, resolved.end());
  }

  @Test
  @DisplayName("個別変更で休みへの変更が反映される")
  void testAdjustmentToLeave() {
    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime baseStart = LocalTime.of(9, 0);
    LocalTime baseEnd = LocalTime.of(18, 0);
    DailyWish baseWish = new DailyWish(false, baseStart, baseEnd);
    baseShifts.put(DayOfWeek.MONDAY, baseWish);
    baseShifts.put(DayOfWeek.TUESDAY, baseWish);
    baseShifts.put(DayOfWeek.WEDNESDAY, baseWish);
    baseShifts.put(DayOfWeek.THURSDAY, baseWish);
    baseShifts.put(DayOfWeek.FRIDAY, baseWish);

    EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);

    LocalDate tuesdayDate = LocalDate.of(2024, 9, 3);
    DailyWish adjustWish = new DailyWish(true, null, null);
    ShiftAdjustment adjustment = new ShiftAdjustment(tuesdayDate, "Taro", adjustWish);
    List<ShiftAdjustment> adjustments = new ArrayList<>();
    adjustments.add(adjustment);

    WishResolver resolver = new WishResolver();
    DailyWish resolved = resolver.resolve(profile, tuesdayDate, adjustments);

    assertTrue(resolved.off());
  }

  @Test
  @DisplayName("同じ日付・氏名の個別変更が複数あれば、後ろのものを採用する")
  void testLastAdjustmentWins() {
    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime baseStart = LocalTime.of(9, 0);
    LocalTime baseEnd = LocalTime.of(18, 0);
    DailyWish baseWish = new DailyWish(false, baseStart, baseEnd);
    baseShifts.put(DayOfWeek.MONDAY, baseWish);
    baseShifts.put(DayOfWeek.TUESDAY, baseWish);
    baseShifts.put(DayOfWeek.WEDNESDAY, baseWish);
    baseShifts.put(DayOfWeek.THURSDAY, baseWish);
    baseShifts.put(DayOfWeek.FRIDAY, baseWish);

    EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);

    LocalDate tuesdayDate = LocalDate.of(2024, 9, 3);
    LocalTime adjust1Start = LocalTime.of(10, 0);
    LocalTime adjust1End = LocalTime.of(17, 0);
    DailyWish adjustWish1 = new DailyWish(false, adjust1Start, adjust1End);
    ShiftAdjustment adjustment1 = new ShiftAdjustment(tuesdayDate, "Taro", adjustWish1);

    LocalTime adjust2Start = LocalTime.of(11, 0);
    LocalTime adjust2End = LocalTime.of(16, 0);
    DailyWish adjustWish2 = new DailyWish(false, adjust2Start, adjust2End);
    ShiftAdjustment adjustment2 = new ShiftAdjustment(tuesdayDate, "Taro", adjustWish2);

    List<ShiftAdjustment> adjustments = new ArrayList<>();
    adjustments.add(adjustment1);
    adjustments.add(adjustment2);

    WishResolver resolver = new WishResolver();
    DailyWish resolved = resolver.resolve(profile, tuesdayDate, adjustments);

    // 後ろの個別変更が採用される
    assertFalse(resolved.off());
    assertEquals(adjust2Start, resolved.start());
    assertEquals(adjust2End, resolved.end());
  }
}
