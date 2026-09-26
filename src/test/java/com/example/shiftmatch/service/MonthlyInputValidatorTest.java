package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.InputError;
import com.example.shiftmatch.domain.MonthlyShiftInput;
import com.example.shiftmatch.domain.ShiftAdjustment;
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

@DisplayName("[V-1][V-2][V-3][V-8][V-9] 入力チェック")
class MonthlyInputValidatorTest {

  @Test
  @DisplayName("[V-1] 空行がエラーにならず除外される")
  void testEmptyNameNotError() {
    HolidayService holidayService = mock(HolidayService.class);
    when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
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
    when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
    when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
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
    when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
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
    when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
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
    when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
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

  @Test
  @DisplayName("[V-5] 有効な従業員が 12 名ならエラーにならない")
  void testMaxEmployeesOk() {
    HolidayService holidayService = mock(HolidayService.class);
    when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
    MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    for (DayOfWeek day :
        new DayOfWeek[] {
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY
        }) {
      baseShifts.put(day, wish);
    }

    List<EmployeeProfile> profiles = new ArrayList<>();
    for (int i = 0; i < 12; i++) {
      profiles.add(new EmployeeProfile("Employee" + i, EmploymentType.FULL_TIME, baseShifts));
    }

    MonthlyShiftInput input =
        new MonthlyShiftInput(YearMonth.of(2024, 9), profiles, new ArrayList<>());

    List<InputError> errors = validator.validate(input);

    assertTrue(errors.isEmpty());
  }

  @Test
  @DisplayName("[V-5] 有効な従業員が 13 名ならエラー")
  void testExceedsMaxEmployees() {
    HolidayService holidayService = mock(HolidayService.class);
    when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
    MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    for (DayOfWeek day :
        new DayOfWeek[] {
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY
        }) {
      baseShifts.put(day, wish);
    }

    List<EmployeeProfile> profiles = new ArrayList<>();
    for (int i = 0; i < 13; i++) {
      profiles.add(new EmployeeProfile("Employee" + i, EmploymentType.FULL_TIME, baseShifts));
    }

    MonthlyShiftInput input =
        new MonthlyShiftInput(YearMonth.of(2024, 9), profiles, new ArrayList<>());

    List<InputError> errors = validator.validate(input);

    assertEquals(1, errors.size());
    assertEquals("V-5", errors.get(0).code());
  }

  @Test
  @DisplayName("[V-6] 従業員名が 255 文字ならエラーにならない")
  void testMaxNameLength() {
    HolidayService holidayService = mock(HolidayService.class);
    when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
    MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    for (DayOfWeek day :
        new DayOfWeek[] {
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY
        }) {
      baseShifts.put(day, wish);
    }

    String longName = "a".repeat(255);
    EmployeeProfile profile = new EmployeeProfile(longName, EmploymentType.FULL_TIME, baseShifts);

    MonthlyShiftInput input =
        new MonthlyShiftInput(YearMonth.of(2024, 9), List.of(profile), new ArrayList<>());

    List<InputError> errors = validator.validate(input);

    assertTrue(errors.isEmpty());
  }

  @Test
  @DisplayName("[V-6] 従業員名が 256 文字ならエラー")
  void testExceedsNameLength() {
    HolidayService holidayService = mock(HolidayService.class);
    when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
    MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    for (DayOfWeek day :
        new DayOfWeek[] {
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY
        }) {
      baseShifts.put(day, wish);
    }

    String longName = "a".repeat(256);
    EmployeeProfile profile = new EmployeeProfile(longName, EmploymentType.FULL_TIME, baseShifts);

    MonthlyShiftInput input =
        new MonthlyShiftInput(YearMonth.of(2024, 9), List.of(profile), new ArrayList<>());

    List<InputError> errors = validator.validate(input);

    assertEquals(1, errors.size());
    assertEquals("V-6", errors.get(0).code());
  }

  @Test
  @DisplayName("[V-7] 雇用区分が null ならエラー")
  void testInvalidEmploymentType() {
    HolidayService holidayService = mock(HolidayService.class);
    when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
    MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    for (DayOfWeek day :
        new DayOfWeek[] {
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY
        }) {
      baseShifts.put(day, wish);
    }

    EmployeeProfile profile = new EmployeeProfile("Taro", null, baseShifts);

    MonthlyShiftInput input =
        new MonthlyShiftInput(YearMonth.of(2024, 9), List.of(profile), new ArrayList<>());

    List<InputError> errors = validator.validate(input);

    assertEquals(1, errors.size());
    assertEquals("V-7", errors.get(0).code());
  }

  @Test
  @DisplayName("[V-8] 対象月が判定できない場合はエラー")
  void testUnsupportedMonth() {
    HolidayService holidayService = mock(HolidayService.class);
    when(holidayService.isSupported(YearMonth.of(2025, 1))).thenReturn(false);

    MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    for (DayOfWeek day :
        new DayOfWeek[] {
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY
        }) {
      baseShifts.put(day, wish);
    }

    EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);

    MonthlyShiftInput input =
        new MonthlyShiftInput(YearMonth.of(2025, 1), List.of(profile), new ArrayList<>());

    List<InputError> errors = validator.validate(input);

    assertEquals(1, errors.size());
    assertEquals("V-8", errors.get(0).code());
  }

  @Test
  @DisplayName("[V-8] V-8 エラーがあるとき businessDays が呼ばれない")
  void testV8ErrorSkipsBusinessDaysCall() {
    HolidayService holidayService = mock(HolidayService.class);
    when(holidayService.isSupported(YearMonth.of(2025, 1))).thenReturn(false);

    MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    for (DayOfWeek day :
        new DayOfWeek[] {
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY
        }) {
      baseShifts.put(day, wish);
    }

    EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);

    MonthlyShiftInput input =
        new MonthlyShiftInput(YearMonth.of(2025, 1), List.of(profile), new ArrayList<>());

    validator.validate(input);

    verify(holidayService, never()).businessDays(YearMonth.of(2025, 1));
  }

  @Test
  @DisplayName("[V-9] 対象月の営業日でない日付の個別変更はエラー")
  void testAdjustmentDateNotBusinessDay() {
    HolidayService holidayService = mock(HolidayService.class);
    when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
    YearMonth month = YearMonth.of(2024, 9);
    LocalDate businessDay = LocalDate.of(2024, 9, 2); // Monday

    when(holidayService.isSupported(month)).thenReturn(true);
    when(holidayService.businessDays(month)).thenReturn(List.of(businessDay));

    MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    for (DayOfWeek day :
        new DayOfWeek[] {
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY
        }) {
      baseShifts.put(day, wish);
    }

    EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);

    // 個別変更で土曜日（営業日外）を指定
    ShiftAdjustment adjustment =
        new ShiftAdjustment(LocalDate.of(2024, 9, 7), "Taro", new DailyWish(false, start, end));

    MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), List.of(adjustment));

    List<InputError> errors = validator.validate(input);

    assertEquals(1, errors.size());
    assertEquals("V-9", errors.get(0).code());
    assertTrue(errors.get(0).message().contains("2024-09-07"));
  }

  @Test
  @DisplayName("[V-9] 対象月以外の日付の個別変更はエラー")
  void testAdjustmentDateOutsideMonth() {
    HolidayService holidayService = mock(HolidayService.class);
    when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
    YearMonth month = YearMonth.of(2024, 9);
    LocalDate businessDay = LocalDate.of(2024, 9, 2); // Monday

    when(holidayService.isSupported(month)).thenReturn(true);
    when(holidayService.businessDays(month)).thenReturn(List.of(businessDay));

    MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    for (DayOfWeek day :
        new DayOfWeek[] {
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY
        }) {
      baseShifts.put(day, wish);
    }

    EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);

    // 個別変更で10月の日付を指定
    ShiftAdjustment adjustment =
        new ShiftAdjustment(LocalDate.of(2024, 10, 1), "Taro", new DailyWish(false, start, end));

    MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), List.of(adjustment));

    List<InputError> errors = validator.validate(input);

    assertEquals(1, errors.size());
    assertEquals("V-9", errors.get(0).code());
  }

  @Test
  @DisplayName("[V-9] 従業員名が一致しない個別変更はエラーにならない")
  void testAdjustmentNameMismatchIgnored() {
    HolidayService holidayService = mock(HolidayService.class);
    when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
    YearMonth month = YearMonth.of(2024, 9);
    LocalDate businessDay = LocalDate.of(2024, 9, 2); // Monday

    when(holidayService.isSupported(month)).thenReturn(true);
    when(holidayService.businessDays(month)).thenReturn(List.of(businessDay));

    MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    for (DayOfWeek day :
        new DayOfWeek[] {
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY
        }) {
      baseShifts.put(day, wish);
    }

    EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);

    // 従業員名が一致しない個別変更で営業日外の日付
    ShiftAdjustment adjustment =
        new ShiftAdjustment(LocalDate.of(2024, 9, 7), "Other", new DailyWish(false, start, end));

    MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), List.of(adjustment));

    List<InputError> errors = validator.validate(input);

    assertTrue(errors.isEmpty());
  }

  @Test
  @DisplayName("[V-2][V-3][V-8][V-9] 複数のエラーが V の番号順に並ぶ")
  void testErrorsSortedByCode() {
    HolidayService holidayService = mock(HolidayService.class);
    YearMonth month = YearMonth.of(2025, 1);

    when(holidayService.isSupported(month)).thenReturn(false);

    MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    baseShifts.put(DayOfWeek.MONDAY, wish);
    baseShifts.put(DayOfWeek.TUESDAY, wish);
    baseShifts.put(DayOfWeek.WEDNESDAY, wish);
    baseShifts.put(DayOfWeek.THURSDAY, wish);
    // FRIDAY が missing (V-3)

    EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);
    EmployeeProfile duplicate = new EmployeeProfile("Taro", EmploymentType.PART_TIME, baseShifts);

    MonthlyShiftInput input =
        new MonthlyShiftInput(month, List.of(profile, duplicate), new ArrayList<>());

    List<InputError> errors = validator.validate(input);

    // V-2: duplicate name, V-3: missing FRIDAY, V-8: unsupported month
    assertTrue(errors.size() >= 2);
    assertTrue(errors.get(0).code().equals("V-2"));
    assertTrue(errors.get(1).code().equals("V-3"));
    // V-9 is skipped because V-8 error exists
    for (InputError error : errors) {
      assertFalse(error.code().equals("V-9"));
    }
  }

  @Test
  @DisplayName("[V-3] 個別変更の開始が null のときエラー")
  void testAdjustmentMissingStart() {
    HolidayService holidayService = mock(HolidayService.class);
    when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
    YearMonth month = YearMonth.of(2024, 9);
    LocalDate businessDay = LocalDate.of(2024, 9, 2); // Monday

    when(holidayService.businessDays(month)).thenReturn(List.of(businessDay));

    MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    for (DayOfWeek day :
        new DayOfWeek[] {
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY
        }) {
      baseShifts.put(day, wish);
    }

    EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);

    ShiftAdjustment adjustment =
        new ShiftAdjustment(businessDay, "Taro", new DailyWish(false, null, end));

    MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), List.of(adjustment));

    List<InputError> errors = validator.validate(input);

    assertEquals(1, errors.size());
    assertEquals("V-3", errors.get(0).code());
    assertTrue(errors.get(0).message().contains("Taro"));
    assertTrue(errors.get(0).message().contains("2024-09-02"));
  }

  @Test
  @DisplayName("[V-3] 個別変更の終了が null のときエラー")
  void testAdjustmentMissingEnd() {
    HolidayService holidayService = mock(HolidayService.class);
    when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
    YearMonth month = YearMonth.of(2024, 9);
    LocalDate businessDay = LocalDate.of(2024, 9, 2); // Monday

    when(holidayService.businessDays(month)).thenReturn(List.of(businessDay));

    MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    for (DayOfWeek day :
        new DayOfWeek[] {
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY
        }) {
      baseShifts.put(day, wish);
    }

    EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);

    ShiftAdjustment adjustment =
        new ShiftAdjustment(businessDay, "Taro", new DailyWish(false, start, null));

    MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), List.of(adjustment));

    List<InputError> errors = validator.validate(input);

    assertEquals(1, errors.size());
    assertEquals("V-3", errors.get(0).code());
    assertTrue(errors.get(0).message().contains("Taro"));
    assertTrue(errors.get(0).message().contains("2024-09-02"));
  }

  @Test
  @DisplayName("[V-3] 個別変更の開始が 30 分単位でないときエラー")
  void testAdjustmentInvalidStartMinutes() {
    HolidayService holidayService = mock(HolidayService.class);
    when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
    YearMonth month = YearMonth.of(2024, 9);
    LocalDate businessDay = LocalDate.of(2024, 9, 2); // Monday

    when(holidayService.businessDays(month)).thenReturn(List.of(businessDay));

    MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    for (DayOfWeek day :
        new DayOfWeek[] {
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY
        }) {
      baseShifts.put(day, wish);
    }

    EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);

    ShiftAdjustment adjustment =
        new ShiftAdjustment(businessDay, "Taro", new DailyWish(false, LocalTime.of(9, 15), end));

    MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), List.of(adjustment));

    List<InputError> errors = validator.validate(input);

    assertEquals(1, errors.size());
    assertEquals("V-3", errors.get(0).code());
    assertTrue(errors.get(0).message().contains("Taro"));
  }

  @Test
  @DisplayName("[V-3] 個別変更の開始 >= 終了のときエラー")
  void testAdjustmentInvalidRange() {
    HolidayService holidayService = mock(HolidayService.class);
    when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
    YearMonth month = YearMonth.of(2024, 9);
    LocalDate businessDay = LocalDate.of(2024, 9, 2); // Monday

    when(holidayService.businessDays(month)).thenReturn(List.of(businessDay));

    MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    for (DayOfWeek day :
        new DayOfWeek[] {
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY
        }) {
      baseShifts.put(day, wish);
    }

    EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);

    ShiftAdjustment adjustment =
        new ShiftAdjustment(businessDay, "Taro", new DailyWish(false, end, start)); // end >= start

    MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), List.of(adjustment));

    List<InputError> errors = validator.validate(input);

    assertEquals(1, errors.size());
    assertEquals("V-3", errors.get(0).code());
    assertTrue(errors.get(0).message().contains("Taro"));
  }

  @Test
  @DisplayName("[V-3] 休みの個別変更は時刻が null でもエラーにならない")
  void testAdjustmentOffIgnoresTime() {
    HolidayService holidayService = mock(HolidayService.class);
    when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
    YearMonth month = YearMonth.of(2024, 9);
    LocalDate businessDay = LocalDate.of(2024, 9, 2); // Monday

    when(holidayService.businessDays(month)).thenReturn(List.of(businessDay));

    MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    for (DayOfWeek day :
        new DayOfWeek[] {
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY
        }) {
      baseShifts.put(day, wish);
    }

    EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);

    ShiftAdjustment adjustment =
        new ShiftAdjustment(businessDay, "Taro", new DailyWish(true, null, null));

    MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), List.of(adjustment));

    List<InputError> errors = validator.validate(input);

    assertTrue(errors.isEmpty());
  }

  @Test
  @DisplayName("[V-3] 従業員名が一致しない個別変更の時間帯は検証されない")
  void testAdjustmentNameMismatchNotValidated() {
    HolidayService holidayService = mock(HolidayService.class);
    when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
    YearMonth month = YearMonth.of(2024, 9);
    LocalDate businessDay = LocalDate.of(2024, 9, 2); // Monday

    when(holidayService.businessDays(month)).thenReturn(List.of(businessDay));

    MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    for (DayOfWeek day :
        new DayOfWeek[] {
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY
        }) {
      baseShifts.put(day, wish);
    }

    EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);

    // 従業員名が一致しない個別変更で無効な時間帯
    ShiftAdjustment adjustment =
        new ShiftAdjustment(businessDay, "Other", new DailyWish(false, null, null));

    MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), List.of(adjustment));

    List<InputError> errors = validator.validate(input);

    assertTrue(errors.isEmpty());
  }
}
