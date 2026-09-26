package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.InputError;
import com.example.shiftmatch.domain.MonthlyShiftInput;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("[V-1][V-2][V-3] 入力チェック")
class MonthlyInputValidatorTest {

  @Test
  @DisplayName("[V-1] 空行がエラーにならず除外される")
  void testEmptyNameNotError() {
    HolidayService holidayService = mock(HolidayService.class);
    MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    baseShifts.put(DayOfWeek.MONDAY, wish);
    baseShifts.put(DayOfWeek.TUESDAY, wish);
    baseShifts.put(DayOfWeek.WEDNESDAY, wish);
    baseShifts.put(DayOfWeek.THURSDAY, wish);
    baseShifts.put(DayOfWeek.FRIDAY, wish);

    EmployeeProfile taroProfile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);
    EmployeeProfile emptyNameProfile =
        new EmployeeProfile("", EmploymentType.FULL_TIME, baseShifts);

    MonthlyShiftInput input =
        new MonthlyShiftInput(
            YearMonth.of(2024, 9), List.of(taroProfile, emptyNameProfile), new ArrayList<>());

    List<InputError> errors = validator.validate(input);

    assertTrue(errors.isEmpty());
  }

  @Test
  @DisplayName("[V-2] 従業員名が重複しているとエラー")
  void testDuplicateName() {
    HolidayService holidayService = mock(HolidayService.class);
    MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    baseShifts.put(DayOfWeek.MONDAY, wish);
    baseShifts.put(DayOfWeek.TUESDAY, wish);
    baseShifts.put(DayOfWeek.WEDNESDAY, wish);
    baseShifts.put(DayOfWeek.THURSDAY, wish);
    baseShifts.put(DayOfWeek.FRIDAY, wish);

    EmployeeProfile profile1 = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);
    EmployeeProfile profile2 = new EmployeeProfile("Taro", EmploymentType.PART_TIME, baseShifts);

    MonthlyShiftInput input =
        new MonthlyShiftInput(
            YearMonth.of(2024, 9), List.of(profile1, profile2), new ArrayList<>());

    List<InputError> errors = validator.validate(input);

    assertEquals(1, errors.size());
    assertEquals("V-2", errors.get(0).code());
    assertTrue(errors.get(0).message().contains("Taro"));
  }

  @Test
  @DisplayName("[V-3] 基本シフトで未選択の時間帯はエラー")
  void testBaseShiftMissingTime() {
    HolidayService holidayService = mock(HolidayService.class);
    MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    baseShifts.put(DayOfWeek.MONDAY, wish);
    baseShifts.put(DayOfWeek.TUESDAY, wish);
    baseShifts.put(DayOfWeek.WEDNESDAY, wish);
    baseShifts.put(DayOfWeek.THURSDAY, wish);
    // FRIDAY が missing

    EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);

    MonthlyShiftInput input =
        new MonthlyShiftInput(YearMonth.of(2024, 9), List.of(profile), new ArrayList<>());

    List<InputError> errors = validator.validate(input);

    assertEquals(1, errors.size());
    assertEquals("V-3", errors.get(0).code());
    assertTrue(errors.get(0).message().contains("金曜日"));
  }

  @Test
  @DisplayName("[V-3] 基本シフトで開始 >= 終了はエラー")
  void testBaseShiftInvalidTimeRange() {
    HolidayService holidayService = mock(HolidayService.class);
    MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    baseShifts.put(DayOfWeek.MONDAY, wish);
    baseShifts.put(DayOfWeek.TUESDAY, wish);
    baseShifts.put(DayOfWeek.WEDNESDAY, wish);
    baseShifts.put(DayOfWeek.THURSDAY, wish);
    baseShifts.put(DayOfWeek.FRIDAY, new DailyWish(false, end, start)); // invalid

    EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);

    MonthlyShiftInput input =
        new MonthlyShiftInput(YearMonth.of(2024, 9), List.of(profile), new ArrayList<>());

    List<InputError> errors = validator.validate(input);

    assertEquals(1, errors.size());
    assertEquals("V-3", errors.get(0).code());
  }

  @Test
  @DisplayName("[V-3] 休みのときは開始・終了が null でもエラーにならない")
  void testLeaveIgnoresTimeRange() {
    HolidayService holidayService = mock(HolidayService.class);
    MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    baseShifts.put(DayOfWeek.MONDAY, wish);
    baseShifts.put(DayOfWeek.TUESDAY, wish);
    baseShifts.put(DayOfWeek.WEDNESDAY, wish);
    baseShifts.put(DayOfWeek.THURSDAY, wish);
    baseShifts.put(DayOfWeek.FRIDAY, new DailyWish(true, null, null));

    EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);

    MonthlyShiftInput input =
        new MonthlyShiftInput(YearMonth.of(2024, 9), List.of(profile), new ArrayList<>());

    List<InputError> errors = validator.validate(input);

    assertTrue(errors.isEmpty());
  }
}
