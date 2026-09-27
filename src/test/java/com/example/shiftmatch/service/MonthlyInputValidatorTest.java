package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("[V-1][V-2][V-3][V-8][V-9] 入力チェック")
class MonthlyInputValidatorTest {

  private static final List<DayOfWeek> WEEKDAYS =
      List.of(
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY);

  private static Map<DayOfWeek, DailyWish> defaultBaseShifts() {
    Map<DayOfWeek, DailyWish> shifts = new EnumMap<>(DayOfWeek.class);
    for (DayOfWeek day : WEEKDAYS) {
      shifts.put(day, new DailyWish(false, LocalTime.of(7, 30), LocalTime.of(18, 30)));
    }
    return shifts;
  }

  private static Map<DayOfWeek, DailyWish> baseShiftsWithOverride(
      DayOfWeek day, DailyWish override) {
    Map<DayOfWeek, DailyWish> shifts = defaultBaseShifts();
    shifts.put(day, override);
    return shifts;
  }

  private static Map<DayOfWeek, DailyWish> baseShiftsMissing(DayOfWeek missingDay) {
    Map<DayOfWeek, DailyWish> shifts = defaultBaseShifts();
    shifts.remove(missingDay);
    return shifts;
  }

  @Nested
  class 正常系 {

    @Test
    @DisplayName("[V-1] Given: 空行を含むとき, When: 入力チェックを実行すると, Then: 空行は処理対象から除外される")
    void emptyNameExcluded() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      EmployeeProfile taroProfile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of());
      EmployeeProfile emptyNameProfile =
          new EmployeeProfile("", EmploymentType.FULL_TIME, Set.of());

      MonthlyShiftInput input =
          new MonthlyShiftInput(
              YearMonth.of(2024, 9), List.of(taroProfile, emptyNameProfile), new ArrayList<>());

      List<InputError> errors = validator.validate(input);

      assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("[V-5] Given: 有効な従業員が 12 名のとき, When: 入力チェックを実行すると, Then: エラーにならない")
    void maxEmployeesOk() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      List<EmployeeProfile> profiles = new ArrayList<>();
      for (int i = 0; i < 12; i++) {
        profiles.add(new EmployeeProfile("Employee" + i, EmploymentType.FULL_TIME, Set.of()));
      }

      MonthlyShiftInput input =
          new MonthlyShiftInput(YearMonth.of(2024, 9), profiles, new ArrayList<>());

      List<InputError> errors = validator.validate(input);

      assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("[V-6] Given: 従業員名が 255 文字のとき, When: 入力チェックを実行すると, Then: エラーにならない")
    void maxNameLengthOk() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      String longName = "a".repeat(255);
      EmployeeProfile profile = new EmployeeProfile(longName, EmploymentType.FULL_TIME, Set.of());

      MonthlyShiftInput input =
          new MonthlyShiftInput(YearMonth.of(2024, 9), List.of(profile), new ArrayList<>());

      List<InputError> errors = validator.validate(input);

      assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("[V-3] Given: 個別変更で休みを指定し時刻が null のとき, When: 入力チェックを実行すると, Then: エラーにならない")
    void adjustmentOffIgnoresTime() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      YearMonth month = YearMonth.of(2024, 9);
      LocalDate businessDay = LocalDate.of(2024, 9, 2); // Monday

      when(holidayService.businessDays(month)).thenReturn(List.of(businessDay));

      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of());

      ShiftAdjustment adjustment =
          new ShiftAdjustment(businessDay, "Taro", new DailyWish(true, null, null));

      MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), List.of(adjustment));

      List<InputError> errors = validator.validate(input);

      assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("[V-3] Given: パートの曜日休みの曜日に基本シフトがないとき, When: 入力チェックを実行すると, Then: エラーにならない")
    void partTimeOffDayWithoutBaseShiftIsNotValidated() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      EmployeeProfile profile =
          new EmployeeProfile(
              "Taro",
              EmploymentType.PART_TIME,
              baseShiftsMissing(DayOfWeek.MONDAY),
              Set.of(DayOfWeek.MONDAY));

      MonthlyShiftInput input =
          new MonthlyShiftInput(YearMonth.of(2024, 9), List.of(profile), new ArrayList<>());

      List<InputError> errors = validator.validate(input);

      assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("[V-3] Given: 従業員名が空の行の基本シフトが不正なとき, When: 入力チェックを実行すると, Then: 検証されずエラーにならない")
    void emptyNameRowBaseShiftIsNotValidated() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      EmployeeProfile emptyProfile =
          new EmployeeProfile(
              "", EmploymentType.FULL_TIME, baseShiftsMissing(DayOfWeek.MONDAY), Set.of());

      MonthlyShiftInput input =
          new MonthlyShiftInput(YearMonth.of(2024, 9), List.of(emptyProfile), new ArrayList<>());

      List<InputError> errors = validator.validate(input);

      assertTrue(errors.isEmpty());
    }
  }

  @Nested
  class V2異常系_重複チェック {

    @Test
    @DisplayName("[V-2] Given: 従業員名が重複しているとき, When: 入力チェックを実行すると, Then: エラーが返される")
    void duplicateNameError() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      EmployeeProfile profile1 = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of());
      EmployeeProfile profile2 = new EmployeeProfile("Taro", EmploymentType.PART_TIME, Set.of());

      MonthlyShiftInput input =
          new MonthlyShiftInput(
              YearMonth.of(2024, 9), List.of(profile1, profile2), new ArrayList<>());

      List<InputError> errors = validator.validate(input);

      assertEquals(1, errors.size());
      assertEquals("V-2", errors.get(0).code());
      assertTrue(errors.get(0).message().contains("Taro"));
    }

    @Test
    @DisplayName("[V-2] Given: 空行を挟んで従業員名が重複しているとき, When: 入力チェックを実行すると, Then: メッセージに両方の行番号が含まれる")
    void duplicateNameWithEmptyRowsInBetween() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      // 1 行目が空行、2 行目が「Taro」、3 行目が空行、4 行目が「Taro」
      EmployeeProfile emptyProfile1 = new EmployeeProfile("", EmploymentType.FULL_TIME, Set.of());
      EmployeeProfile profile1 = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of());
      EmployeeProfile emptyProfile2 = new EmployeeProfile("", EmploymentType.FULL_TIME, Set.of());
      EmployeeProfile profile2 = new EmployeeProfile("Taro", EmploymentType.PART_TIME, Set.of());

      MonthlyShiftInput input =
          new MonthlyShiftInput(
              YearMonth.of(2024, 9),
              List.of(emptyProfile1, profile1, emptyProfile2, profile2),
              new ArrayList<>());

      List<InputError> errors = validator.validate(input);

      // V-2 エラーのメッセージに「2」と「4」の行番号が含まれていることを確認
      String v2Message = "";
      for (InputError error : errors) {
        if ("V-2".equals(error.code())) {
          v2Message = error.message();
          break;
        }
      }
      assertTrue(
          v2Message.contains("2") && v2Message.contains("4"),
          "V-2 エラーのメッセージが 2 行目と 4 行目の両方を示していません: " + v2Message);
    }
  }

  @Nested
  class V3異常系_時間帯チェック {

    @Test
    @DisplayName("[V-3] Given: 個別変更の開始が null のとき, When: 入力チェックを実行すると, Then: エラーが返される")
    void adjustmentMissingStart() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      YearMonth month = YearMonth.of(2024, 9);
      LocalDate businessDay = LocalDate.of(2024, 9, 2); // Monday

      when(holidayService.businessDays(month)).thenReturn(List.of(businessDay));

      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      LocalTime end = LocalTime.of(18, 0);

      EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of());

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
    @DisplayName("[V-3] Given: 個別変更の終了が null のとき, When: 入力チェックを実行すると, Then: エラーが返される")
    void adjustmentMissingEnd() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      YearMonth month = YearMonth.of(2024, 9);
      LocalDate businessDay = LocalDate.of(2024, 9, 2); // Monday

      when(holidayService.businessDays(month)).thenReturn(List.of(businessDay));

      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      LocalTime start = LocalTime.of(9, 0);

      EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of());

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
    @DisplayName("[V-3] Given: 個別変更の開始が 30 分単位でないとき, When: 入力チェックを実行すると, Then: エラーが返される")
    void adjustmentInvalidStartMinutes() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      YearMonth month = YearMonth.of(2024, 9);
      LocalDate businessDay = LocalDate.of(2024, 9, 2); // Monday

      when(holidayService.businessDays(month)).thenReturn(List.of(businessDay));

      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      LocalTime end = LocalTime.of(18, 0);

      EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of());

      ShiftAdjustment adjustment =
          new ShiftAdjustment(businessDay, "Taro", new DailyWish(false, LocalTime.of(9, 15), end));

      MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), List.of(adjustment));

      List<InputError> errors = validator.validate(input);

      assertEquals(1, errors.size());
      assertEquals("V-3", errors.get(0).code());
      assertTrue(errors.get(0).message().contains("Taro"));
    }

    @Test
    @DisplayName("[V-3] Given: 個別変更の開始 >= 終了のとき, When: 入力チェックを実行すると, Then: エラーが返される")
    void adjustmentInvalidRange() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      YearMonth month = YearMonth.of(2024, 9);
      LocalDate businessDay = LocalDate.of(2024, 9, 2); // Monday

      when(holidayService.businessDays(month)).thenReturn(List.of(businessDay));

      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      LocalTime start = LocalTime.of(9, 0);
      LocalTime end = LocalTime.of(18, 0);

      EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of());

      ShiftAdjustment adjustment =
          new ShiftAdjustment(
              businessDay, "Taro", new DailyWish(false, end, start)); // end >= start

      MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), List.of(adjustment));

      List<InputError> errors = validator.validate(input);

      assertEquals(1, errors.size());
      assertEquals("V-3", errors.get(0).code());
      assertTrue(errors.get(0).message().contains("Taro"));
    }
  }

  @Nested
  class V3異常系_基本シフトチェック {

    @Test
    @DisplayName("[V-3] Given: 月曜日の基本シフトが未選択のとき, When: 入力チェックを実行すると, Then: エラーが返される")
    void baseShiftMissing() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      EmployeeProfile profile =
          new EmployeeProfile(
              "Taro", EmploymentType.FULL_TIME, baseShiftsMissing(DayOfWeek.MONDAY), Set.of());

      MonthlyShiftInput input =
          new MonthlyShiftInput(YearMonth.of(2024, 9), List.of(profile), new ArrayList<>());

      List<InputError> errors = validator.validate(input);

      assertEquals(1, errors.size());
      assertEquals("V-3", errors.get(0).code());
      assertTrue(errors.get(0).message().contains("月曜日"));
      assertTrue(errors.get(0).message().contains("Taro"));
      assertTrue(errors.get(0).message().contains("未選択"));
    }

    @Test
    @DisplayName("[V-3] Given: 月曜日の基本シフトの開始が 8:15 のとき, When: 入力チェックを実行すると, Then: エラーが返される")
    void baseShiftNotHalfHourUnit() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      EmployeeProfile profile =
          new EmployeeProfile(
              "Taro",
              EmploymentType.FULL_TIME,
              baseShiftsWithOverride(
                  DayOfWeek.MONDAY, new DailyWish(false, LocalTime.of(8, 15), LocalTime.of(17, 0))),
              Set.of());

      MonthlyShiftInput input =
          new MonthlyShiftInput(YearMonth.of(2024, 9), List.of(profile), new ArrayList<>());

      List<InputError> errors = validator.validate(input);

      assertEquals(1, errors.size());
      assertEquals("V-3", errors.get(0).code());
      assertTrue(errors.get(0).message().contains("30 分単位"));
    }

    @Test
    @DisplayName("[V-3] Given: 月曜日の基本シフトの開始が 7:00 のとき, When: 入力チェックを実行すると, Then: エラーが返される")
    void baseShiftBeforeMinTime() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      EmployeeProfile profile =
          new EmployeeProfile(
              "Taro",
              EmploymentType.FULL_TIME,
              baseShiftsWithOverride(
                  DayOfWeek.MONDAY, new DailyWish(false, LocalTime.of(7, 0), LocalTime.of(17, 0))),
              Set.of());

      MonthlyShiftInput input =
          new MonthlyShiftInput(YearMonth.of(2024, 9), List.of(profile), new ArrayList<>());

      List<InputError> errors = validator.validate(input);

      assertEquals(1, errors.size());
      assertEquals("V-3", errors.get(0).code());
      assertTrue(errors.get(0).message().contains("30 分単位"));
    }

    @Test
    @DisplayName("[V-3] Given: 月曜日の基本シフトの開始が終了以上のとき, When: 入力チェックを実行すると, Then: エラーが返される")
    void baseShiftInvalidRange() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      EmployeeProfile profile =
          new EmployeeProfile(
              "Taro",
              EmploymentType.FULL_TIME,
              baseShiftsWithOverride(
                  DayOfWeek.MONDAY, new DailyWish(false, LocalTime.of(17, 0), LocalTime.of(9, 0))),
              Set.of());

      MonthlyShiftInput input =
          new MonthlyShiftInput(YearMonth.of(2024, 9), List.of(profile), new ArrayList<>());

      List<InputError> errors = validator.validate(input);

      assertEquals(1, errors.size());
      assertEquals("V-3", errors.get(0).code());
      assertTrue(errors.get(0).message().contains("開始時刻が終了時刻以上"));
    }

    @Test
    @DisplayName("[V-3] Given: 基本シフトと個別変更の両方にエラーがあるとき, When: 入力チェックを実行すると, Then: 基本シフトのエラーが先に並ぶ")
    void baseShiftErrorsComeBeforeAdjustmentErrors() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      YearMonth month = YearMonth.of(2024, 9);
      LocalDate businessDay = LocalDate.of(2024, 9, 2); // Monday
      when(holidayService.businessDays(month)).thenReturn(List.of(businessDay));

      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      EmployeeProfile profile =
          new EmployeeProfile(
              "Taro", EmploymentType.FULL_TIME, baseShiftsMissing(DayOfWeek.MONDAY), Set.of());
      ShiftAdjustment adjustment =
          new ShiftAdjustment(businessDay, "Taro", new DailyWish(false, null, LocalTime.of(18, 0)));

      MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), List.of(adjustment));

      List<InputError> errors = validator.validate(input);

      assertEquals(2, errors.size());
      assertEquals("V-3", errors.get(0).code());
      assertTrue(errors.get(0).message().contains("基本シフト"));
      assertEquals("V-3", errors.get(1).code());
      assertTrue(errors.get(1).message().contains("個別変更"));
    }
  }

  @Nested
  class V5異常系_従業員数チェック {

    @Test
    @DisplayName("[V-5] Given: 有効な従業員が 13 名のとき, When: 入力チェックを実行すると, Then: エラーが返される")
    void exceedsMaxEmployees() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      List<EmployeeProfile> profiles = new ArrayList<>();
      for (int i = 0; i < 13; i++) {
        profiles.add(new EmployeeProfile("Employee" + i, EmploymentType.FULL_TIME, Set.of()));
      }

      MonthlyShiftInput input =
          new MonthlyShiftInput(YearMonth.of(2024, 9), profiles, new ArrayList<>());

      List<InputError> errors = validator.validate(input);

      assertEquals(1, errors.size());
      assertEquals("V-5", errors.get(0).code());
    }
  }

  @Nested
  class V6異常系_名前の長さチェック {

    @Test
    @DisplayName("[V-6] Given: 従業員名が 256 文字のとき, When: 入力チェックを実行すると, Then: エラーが返される")
    void exceedsNameLength() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      String longName = "a".repeat(256);
      EmployeeProfile profile = new EmployeeProfile(longName, EmploymentType.FULL_TIME, Set.of());

      MonthlyShiftInput input =
          new MonthlyShiftInput(YearMonth.of(2024, 9), List.of(profile), new ArrayList<>());

      List<InputError> errors = validator.validate(input);

      assertEquals(1, errors.size());
      assertEquals("V-6", errors.get(0).code());
    }

    @Test
    @DisplayName(
        "[V-6] Given: 1 行目が空で 2 行目の名前が 256 文字のとき, When: 入力チェックを実行すると, Then: エラーメッセージが 2 行目を示す")
    void nameLengthWithEmptyFirstRow() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      EmployeeProfile emptyProfile = new EmployeeProfile("", EmploymentType.FULL_TIME, Set.of());
      String longName = "a".repeat(256);
      EmployeeProfile longNameProfile =
          new EmployeeProfile(longName, EmploymentType.FULL_TIME, Set.of());

      MonthlyShiftInput input =
          new MonthlyShiftInput(
              YearMonth.of(2024, 9), List.of(emptyProfile, longNameProfile), new ArrayList<>());

      List<InputError> errors = validator.validate(input);

      // V-6 エラーのメッセージに「2 行目」が含まれていることを確認
      boolean hasLineTwo = false;
      for (InputError error : errors) {
        if ("V-6".equals(error.code()) && error.message().contains("2")) {
          hasLineTwo = true;
          break;
        }
      }
      assertTrue(hasLineTwo, "V-6 エラーのメッセージが 2 行目を示していません");
    }
  }

  @Nested
  class V7異常系_雇用区分チェック {

    @Test
    @DisplayName("[V-7] Given: 雇用区分が null のとき, When: 入力チェックを実行すると, Then: エラーが返される")
    void invalidEmploymentType() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      EmployeeProfile profile = new EmployeeProfile("Taro", null, Set.of());

      MonthlyShiftInput input =
          new MonthlyShiftInput(YearMonth.of(2024, 9), List.of(profile), new ArrayList<>());

      List<InputError> errors = validator.validate(input);

      assertEquals(1, errors.size());
      assertEquals("V-7", errors.get(0).code());
    }

    @Test
    @DisplayName(
        "[V-7] Given: 1 行目が空で 2 行目の雇用区分が null のとき, When: 入力チェックを実行すると, Then: エラーメッセージが 2 行目を示す")
    void employmentTypeWithEmptyFirstRow() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      EmployeeProfile emptyProfile = new EmployeeProfile("", EmploymentType.FULL_TIME, Set.of());
      EmployeeProfile invalidProfile = new EmployeeProfile("Taro", null, Set.of());

      MonthlyShiftInput input =
          new MonthlyShiftInput(
              YearMonth.of(2024, 9), List.of(emptyProfile, invalidProfile), new ArrayList<>());

      List<InputError> errors = validator.validate(input);

      // V-7 エラーのメッセージに「2 行目」が含まれていることを確認
      boolean hasLineTwo = false;
      for (InputError error : errors) {
        if ("V-7".equals(error.code()) && error.message().contains("2")) {
          hasLineTwo = true;
          break;
        }
      }
      assertTrue(hasLineTwo, "V-7 エラーのメッセージが 2 行目を示していません");
    }
  }

  @Nested
  class V8異常系_対象月チェック {

    @Test
    @DisplayName("[V-8] Given: 対象月がサポートされていないとき, When: 入力チェックを実行すると, Then: エラーが返される")
    void unsupportedMonth() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2025, 1))).thenReturn(false);

      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of());

      MonthlyShiftInput input =
          new MonthlyShiftInput(YearMonth.of(2025, 1), List.of(profile), new ArrayList<>());

      List<InputError> errors = validator.validate(input);

      assertEquals(1, errors.size());
      assertEquals("V-8", errors.get(0).code());
    }

    @Test
    @DisplayName("[V-8] Given: V-8 エラーがあるとき, When: 入力チェックを実行すると, Then: businessDays が呼ばれない")
    void v8ErrorSkipsBusinessDaysCall() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2025, 1))).thenReturn(false);

      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of());

      MonthlyShiftInput input =
          new MonthlyShiftInput(YearMonth.of(2025, 1), List.of(profile), new ArrayList<>());

      validator.validate(input);

      verify(holidayService, never()).businessDays(YearMonth.of(2025, 1));
    }

    @Test
    @DisplayName("[V-8] Given: 対象月が null のとき, When: 入力チェックを実行すると, Then: エラーが返される")
    void nullMonthError() {
      HolidayService holidayService = mock(HolidayService.class);
      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of());

      MonthlyShiftInput input = new MonthlyShiftInput(null, List.of(profile), new ArrayList<>());

      List<InputError> errors = validator.validate(input);

      assertEquals(1, errors.size());
      assertEquals("V-8", errors.get(0).code());
    }

    @Test
    @DisplayName("[V-8] Given: 対象月が null のとき, When: 入力チェックを実行すると, Then: HolidayService が呼ばれない")
    void nullMonthSkipsHolidayService() {
      HolidayService holidayService = mock(HolidayService.class);
      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of());

      MonthlyShiftInput input = new MonthlyShiftInput(null, List.of(profile), new ArrayList<>());

      validator.validate(input);

      verify(holidayService, never()).isSupported(any());
      verify(holidayService, never()).businessDays(any());
    }
  }

  @Nested
  class V9異常系_個別変更の日付チェック {

    @Test
    @DisplayName("[V-9] Given: 個別変更の日付が営業日でないとき, When: 入力チェックを実行すると, Then: エラーが返される")
    void adjustmentDateNotBusinessDay() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      YearMonth month = YearMonth.of(2024, 9);
      LocalDate businessDay = LocalDate.of(2024, 9, 2); // Monday

      when(holidayService.isSupported(month)).thenReturn(true);
      when(holidayService.businessDays(month)).thenReturn(List.of(businessDay));

      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      LocalTime start = LocalTime.of(9, 0);
      LocalTime end = LocalTime.of(18, 0);

      EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of());

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
    @DisplayName("[V-9] Given: 個別変更の日付が対象月外のとき, When: 入力チェックを実行すると, Then: エラーが返される")
    void adjustmentDateOutsideMonth() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      YearMonth month = YearMonth.of(2024, 9);
      LocalDate businessDay = LocalDate.of(2024, 9, 2); // Monday

      when(holidayService.isSupported(month)).thenReturn(true);
      when(holidayService.businessDays(month)).thenReturn(List.of(businessDay));

      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      LocalTime start = LocalTime.of(9, 0);
      LocalTime end = LocalTime.of(18, 0);

      EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of());

      // 個別変更で10月の日付を指定
      ShiftAdjustment adjustment =
          new ShiftAdjustment(LocalDate.of(2024, 10, 1), "Taro", new DailyWish(false, start, end));

      MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), List.of(adjustment));

      List<InputError> errors = validator.validate(input);

      assertEquals(1, errors.size());
      assertEquals("V-9", errors.get(0).code());
    }
  }

  @Nested
  class その他 {

    @Test
    @DisplayName("[V-1] Given: 従業員名が null のとき, When: 入力チェックを実行すると, Then: 除外されエラーにならない")
    void nullNameExcluded() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      EmployeeProfile taroProfile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of());
      EmployeeProfile nullNameProfile =
          new EmployeeProfile(null, EmploymentType.FULL_TIME, Set.of());

      MonthlyShiftInput input =
          new MonthlyShiftInput(
              YearMonth.of(2024, 9), List.of(taroProfile, nullNameProfile), new ArrayList<>());

      List<InputError> errors = validator.validate(input);

      assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("[V-9] Given: 従業員名が一致しない個別変更のとき, When: 入力チェックを実行すると, Then: エラーにならない")
    void adjustmentNameMismatchIgnored() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      YearMonth month = YearMonth.of(2024, 9);
      LocalDate businessDay = LocalDate.of(2024, 9, 2); // Monday

      when(holidayService.isSupported(month)).thenReturn(true);
      when(holidayService.businessDays(month)).thenReturn(List.of(businessDay));

      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      LocalTime start = LocalTime.of(9, 0);
      LocalTime end = LocalTime.of(18, 0);

      EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of());

      // 従業員名が一致しない個別変更で営業日外の日付
      ShiftAdjustment adjustment =
          new ShiftAdjustment(LocalDate.of(2024, 9, 7), "Other", new DailyWish(false, start, end));

      MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), List.of(adjustment));

      List<InputError> errors = validator.validate(input);

      assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName("[V-3] Given: 従業員名が一致しない個別変更で無効な時間帯のとき, When: 入力チェックを実行すると, Then: 検証されずエラーにならない")
    void adjustmentNameMismatchNotValidated() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(YearMonth.of(2024, 9))).thenReturn(true);
      YearMonth month = YearMonth.of(2024, 9);
      LocalDate businessDay = LocalDate.of(2024, 9, 2); // Monday

      when(holidayService.businessDays(month)).thenReturn(List.of(businessDay));

      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of());

      // 従業員名が一致しない個別変更で無効な時間帯
      ShiftAdjustment adjustment =
          new ShiftAdjustment(businessDay, "Other", new DailyWish(false, null, null));

      MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), List.of(adjustment));

      List<InputError> errors = validator.validate(input);

      assertTrue(errors.isEmpty());
    }

    @Test
    @DisplayName(
        "[V-2][V-3][V-8][V-9] Given: 複数のエラー条件が重なるとき, When: 入力チェックを実行すると, Then: エラーが V の番号順に並ぶ")
    void errorsSortedByCode() {
      HolidayService holidayService = mock(HolidayService.class);
      YearMonth month = YearMonth.of(2025, 1);

      when(holidayService.isSupported(month)).thenReturn(false);

      MonthlyInputValidator validator = new MonthlyInputValidator(holidayService);

      LocalTime start = LocalTime.of(9, 0);
      LocalTime end = LocalTime.of(18, 0);

      EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of());
      EmployeeProfile duplicate = new EmployeeProfile("Taro", EmploymentType.PART_TIME, Set.of());
      // 個別変更の開始が null (V-3)
      ShiftAdjustment invalidAdjustment =
          new ShiftAdjustment(LocalDate.of(2025, 1, 6), "Taro", new DailyWish(false, start, null));

      MonthlyShiftInput input =
          new MonthlyShiftInput(month, List.of(profile, duplicate), List.of(invalidAdjustment));

      List<InputError> errors = validator.validate(input);

      // V-2: duplicate name, V-3: adjustment without end, V-8: unsupported month
      assertTrue(errors.size() >= 2);
      assertTrue(errors.get(0).code().equals("V-2"));
      assertTrue(errors.get(1).code().equals("V-3"));
      // V-9 is skipped because V-8 error exists
      for (InputError error : errors) {
        assertFalse(error.code().equals("V-9"));
      }
    }
  }
}
