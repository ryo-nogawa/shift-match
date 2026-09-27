package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.FailureReason;
import com.example.shiftmatch.domain.InputError;
import com.example.shiftmatch.domain.InvalidMonthlyInputException;
import com.example.shiftmatch.domain.MonthlyShiftInput;
import com.example.shiftmatch.domain.MonthlyShiftResult;
import com.example.shiftmatch.domain.ShiftAdjustment;
import com.example.shiftmatch.domain.ShiftAssignment;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("[F-3] 営業日ごとの独立した割り当て算出")
class MonthlyShiftServiceImplTest {

  @Nested
  class 正常系 {

    @Test
    @DisplayName(
        "[F-3] Given: businessDays が返した日が複数あるとき, When: create を実行すると, Then: その日だけが結果に含まれ順序も同じ")
    void onlyBusinessDaysInResult() {
      HolidayService holidayService = mock(HolidayService.class);
      ShiftAssignmentService assignmentService = mock(ShiftAssignmentService.class);

      LocalDate day1 = LocalDate.of(2024, 9, 2); // Monday
      LocalDate day2 = LocalDate.of(2024, 9, 3); // Tuesday
      YearMonth month = YearMonth.of(2024, 9);

      when(holidayService.businessDays(month)).thenReturn(List.of(day1, day2));
      when(holidayService.isSupported(month)).thenReturn(true);

      MonthlyInputValidator inputValidator = mock(MonthlyInputValidator.class);
      SelectionRationaleLogger logger = mock(SelectionRationaleLogger.class);
      MonthlyShiftServiceImpl service =
          new MonthlyShiftServiceImpl(holidayService, assignmentService, inputValidator, logger);

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
      MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), new ArrayList<>());

      MonthlyShiftResult result = service.create(input);

      assertEquals(month, result.month());
      assertEquals(2, result.days().size());
      assertEquals(day1, result.days().get(0).date());
      assertEquals(day2, result.days().get(1).date());
    }

    @Test
    @DisplayName("[V-1] Given: 従業員名が空の行を含むとき, When: create を実行すると, Then: 空行は処理対象から除外される")
    void emptyNameExcluded() {
      HolidayService holidayService = mock(HolidayService.class);
      ShiftAssignmentService assignmentService = mock(ShiftAssignmentService.class);

      LocalDate day1 = LocalDate.of(2024, 9, 2);
      YearMonth month = YearMonth.of(2024, 9);

      when(holidayService.businessDays(month)).thenReturn(List.of(day1));
      when(holidayService.isSupported(month)).thenReturn(true);

      MonthlyInputValidator inputValidator = mock(MonthlyInputValidator.class);
      SelectionRationaleLogger logger = mock(SelectionRationaleLogger.class);
      MonthlyShiftServiceImpl service =
          new MonthlyShiftServiceImpl(holidayService, assignmentService, inputValidator, logger);

      Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
      LocalTime start = LocalTime.of(9, 0);
      LocalTime end = LocalTime.of(18, 0);
      DailyWish wish = new DailyWish(false, start, end);
      baseShifts.put(DayOfWeek.MONDAY, wish);
      baseShifts.put(DayOfWeek.TUESDAY, wish);
      baseShifts.put(DayOfWeek.WEDNESDAY, wish);
      baseShifts.put(DayOfWeek.THURSDAY, wish);
      baseShifts.put(DayOfWeek.FRIDAY, wish);

      EmployeeProfile taroProfile =
          new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);
      Map<DayOfWeek, DailyWish> emptyNameBaseShifts = new HashMap<>(baseShifts);
      EmployeeProfile emptyNameProfile =
          new EmployeeProfile("", EmploymentType.FULL_TIME, emptyNameBaseShifts);

      MonthlyShiftInput input =
          new MonthlyShiftInput(month, List.of(taroProfile, emptyNameProfile), new ArrayList<>());

      MonthlyShiftResult result = service.create(input);

      // verify empty name is excluded
      assertEquals(1, result.days().size());
      DailyShiftResult dayResult = result.days().get(0);
      assertEquals(1, dayResult.availableCount());
    }
  }

  @Nested
  class V4異常系_利用可能人数チェック {

    @Test
    @DisplayName("[V-4] Given: 勤務できる人が 8 名未満のときの日があるとき, When: create を実行すると, Then: その日は割り当て結果がない")
    void unsuccessfulWhenFewerThan8Available() {
      HolidayService holidayService = mock(HolidayService.class);

      LocalDate day1 = LocalDate.of(2024, 9, 2); // 8 名可能
      LocalDate day2 = LocalDate.of(2024, 9, 3); // 7 名可能
      YearMonth month = YearMonth.of(2024, 9);

      when(holidayService.businessDays(month)).thenReturn(List.of(day1, day2));
      when(holidayService.isSupported(month)).thenReturn(true);

      Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
      LocalTime start = LocalTime.of(9, 0);
      LocalTime end = LocalTime.of(18, 0);
      DailyWish wish = new DailyWish(false, start, end);
      baseShifts.put(DayOfWeek.MONDAY, wish);
      baseShifts.put(DayOfWeek.TUESDAY, wish);
      baseShifts.put(DayOfWeek.WEDNESDAY, wish);
      baseShifts.put(DayOfWeek.THURSDAY, wish);
      baseShifts.put(DayOfWeek.FRIDAY, wish);

      // 8 人の従業員
      List<EmployeeProfile> employees = new ArrayList<>();
      for (int i = 0; i < 8; i++) {
        employees.add(new EmployeeProfile("Employee" + i, EmploymentType.FULL_TIME, baseShifts));
      }

      // 火曜日（day2）だけ従業員 0 が休み（7 名だけ勤務可能）
      LocalDate tuesdayDate = LocalDate.of(2024, 9, 3);
      DailyWish offWish = new DailyWish(true, null, null);
      ShiftAdjustment adjustment = new ShiftAdjustment(tuesdayDate, "Employee0", offWish);

      MonthlyShiftInput input = new MonthlyShiftInput(month, employees, List.of(adjustment));

      ShiftAssignmentService assignmentService = mock(ShiftAssignmentService.class);
      when(assignmentService.assign(anyList()))
          .thenReturn(createDummyAssignmentResult())
          .thenReturn(Optional.empty());

      MonthlyInputValidator inputValidator = mock(MonthlyInputValidator.class);
      SelectionRationaleLogger logger = mock(SelectionRationaleLogger.class);
      MonthlyShiftServiceImpl service =
          new MonthlyShiftServiceImpl(holidayService, assignmentService, inputValidator, logger);

      MonthlyShiftResult result = service.create(input);

      assertEquals(2, result.days().size());
      DailyShiftResult day1Result = result.days().get(0);
      DailyShiftResult day2Result = result.days().get(1);

      // 月曜日は 8 人が勤務可能
      assertEquals(8, day1Result.availableCount());
      assertTrue(day1Result.assignment().isPresent(), "day1 should have an assignment");

      // 火曜日は 7 人が勤務可能
      assertEquals(7, day2Result.availableCount());
      assertTrue(day2Result.assignment().isEmpty(), "day2 should not have an assignment");

      verify(assignmentService, times(2)).assign(anyList());
    }
  }

  @Nested
  class V3V9異常系_検証エラー {

    @Test
    @DisplayName(
        "[V-3] Given: 基本シフト検証エラーがあるとき, When: create を実行すると, Then: InvalidMonthlyInputException を投げ"
            + " assign を呼ばない")
    void validationErrorThrowsException() {
      YearMonth month = YearMonth.of(2024, 9);
      LocalDate businessDay = LocalDate.of(2024, 9, 2);
      LocalTime start = LocalTime.of(9, 0);
      LocalTime end = LocalTime.of(18, 0);

      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(month)).thenReturn(true);
      when(holidayService.businessDays(month)).thenReturn(List.of(businessDay));

      Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
      DailyWish wish = new DailyWish(false, start, end);
      baseShifts.put(DayOfWeek.MONDAY, wish);
      baseShifts.put(DayOfWeek.TUESDAY, wish);
      baseShifts.put(DayOfWeek.WEDNESDAY, wish);
      baseShifts.put(DayOfWeek.THURSDAY, wish);
      // FRIDAY missing - V-3 error

      EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);
      MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), new ArrayList<>());

      ShiftAssignmentService assignmentService = mock(ShiftAssignmentService.class);
      MonthlyInputValidator inputValidator = mock(MonthlyInputValidator.class);
      // V-3 エラーを返すようにスタブを設定
      InputError error = new InputError("V-3", "基本シフト：金曜日が未選択です（Taro、1 行目）");
      when(inputValidator.validate(input)).thenReturn(List.of(error));

      SelectionRationaleLogger logger = mock(SelectionRationaleLogger.class);
      MonthlyShiftServiceImpl service =
          new MonthlyShiftServiceImpl(holidayService, assignmentService, inputValidator, logger);
      assertThrows(InvalidMonthlyInputException.class, () -> service.create(input));

      // verify assign was never called
      verifyNoInteractions(assignmentService);
    }

    @Test
    @DisplayName(
        "[V-9] Given: 個別変更検証エラーがあるとき, When: create を実行すると, Then: InvalidMonthlyInputException を投げ"
            + " assign を呼ばない")
    void v9ValidationErrorThrowsException() {
      YearMonth month = YearMonth.of(2024, 9);
      LocalDate businessDay = LocalDate.of(2024, 9, 2);
      LocalTime start = LocalTime.of(9, 0);
      LocalTime end = LocalTime.of(18, 0);
      DailyWish wish = new DailyWish(false, start, end);

      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(month)).thenReturn(true);
      when(holidayService.businessDays(month)).thenReturn(List.of(businessDay));

      Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
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

      // 営業日外の日付で個別変更 - V-9 error
      ShiftAdjustment adjustment = new ShiftAdjustment(LocalDate.of(2024, 9, 7), "Taro", wish);
      MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), List.of(adjustment));

      ShiftAssignmentService assignmentService = mock(ShiftAssignmentService.class);
      MonthlyInputValidator inputValidator = mock(MonthlyInputValidator.class);
      // V-9 エラーを返すようにスタブを設定
      InputError error = new InputError("V-9", "個別変更の日付が対象月の営業日ではありません（2024-09-07）");
      when(inputValidator.validate(input)).thenReturn(List.of(error));

      SelectionRationaleLogger logger = mock(SelectionRationaleLogger.class);
      MonthlyShiftServiceImpl service =
          new MonthlyShiftServiceImpl(holidayService, assignmentService, inputValidator, logger);

      assertThrows(InvalidMonthlyInputException.class, () -> service.create(input));

      // verify assign was never called
      verifyNoInteractions(assignmentService);
    }
  }

  @Nested
  class その他 {

    @Test
    @DisplayName("[7.2 節] Given: 複数の営業日があるとき, When: create を実行すると, Then: 営業日ごとにログを出力する")
    void createLogsForEachBusinessDay() {
      LocalDate day1 = LocalDate.of(2024, 9, 2); // Monday
      LocalDate day2 = LocalDate.of(2024, 9, 3); // Tuesday
      YearMonth month = YearMonth.of(2024, 9);

      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.businessDays(month)).thenReturn(List.of(day1, day2));
      when(holidayService.isSupported(month)).thenReturn(true);

      ShiftAssignmentService assignmentService = mock(ShiftAssignmentService.class);
      when(assignmentService.assign(any())).thenReturn(java.util.Optional.empty());

      MonthlyInputValidator inputValidator = mock(MonthlyInputValidator.class);
      when(inputValidator.validate(any())).thenReturn(new ArrayList<>());

      SelectionRationaleLogger rationaleLogger = mock(SelectionRationaleLogger.class);

      Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
      LocalTime start = LocalTime.of(9, 0);
      LocalTime end = LocalTime.of(18, 0);
      baseShifts.put(DayOfWeek.MONDAY, new DailyWish(false, start, end));
      baseShifts.put(DayOfWeek.TUESDAY, new DailyWish(false, start, end));
      baseShifts.put(DayOfWeek.WEDNESDAY, new DailyWish(false, start, end));
      baseShifts.put(DayOfWeek.THURSDAY, new DailyWish(false, start, end));
      baseShifts.put(DayOfWeek.FRIDAY, new DailyWish(false, start, end));

      EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, baseShifts);

      MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), List.of());

      MonthlyShiftServiceImpl service =
          new MonthlyShiftServiceImpl(
              holidayService, assignmentService, inputValidator, rationaleLogger);
      service.create(input);

      // verify logger.log was called for each business day
      verify(rationaleLogger).log(eq(day1), any());
      verify(rationaleLogger).log(eq(day2), any());
    }
  }

  @Nested
  class パートの週上限 {

    private final YearMonth october = YearMonth.of(2026, 10);

    private List<LocalDate> octoberBusinessDays() {
      List<LocalDate> days = new ArrayList<>();
      for (int d = 1; d <= 31; d++) {
        LocalDate date = LocalDate.of(2026, 10, d);
        boolean weekend =
            date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
        if (!weekend && d != 12) {
          days.add(date);
        }
      }
      return days;
    }

    private MonthlyShiftServiceImpl realService() {
      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.businessDays(october)).thenReturn(octoberBusinessDays());
      MonthlyInputValidator inputValidator = mock(MonthlyInputValidator.class);
      when(inputValidator.validate(any())).thenReturn(new ArrayList<>());
      return new MonthlyShiftServiceImpl(
          holidayService,
          new ShiftAssignmentServiceImpl(),
          inputValidator,
          mock(SelectionRationaleLogger.class));
    }

    private List<EmployeeProfile> profiles(EmploymentType type, String prefix, int count) {
      Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
      for (DayOfWeek day :
          List.of(
              DayOfWeek.MONDAY,
              DayOfWeek.TUESDAY,
              DayOfWeek.WEDNESDAY,
              DayOfWeek.THURSDAY,
              DayOfWeek.FRIDAY)) {
        baseShifts.put(day, new DailyWish(false, LocalTime.of(7, 30), LocalTime.of(18, 30)));
      }
      List<EmployeeProfile> list = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        list.add(new EmployeeProfile(prefix + i, type, baseShifts));
      }
      return list;
    }

    private int netMinutes(DailyShiftResult day, String name) {
      return day.assignment().stream()
          .flatMap(a -> a.assignments().stream())
          .filter(a -> a.employee().name().equals(name))
          .mapToInt(a -> a.slot().netWorkMinutes())
          .sum();
    }

    private static LocalDate mondayOf(LocalDate date) {
      return date.with(DayOfWeek.MONDAY);
    }

    @Test
    @DisplayName(
        "[H-4] Given: 2026 年 10 月にパートを含む入力, When: create を実行すると, Then: 各週のパートの実働合計が 1200 分以内")
    void weeklyTotalOfPartsIsWithinLimit() {
      List<EmployeeProfile> all = new ArrayList<>(profiles(EmploymentType.PART_TIME, "P", 9));
      MonthlyShiftResult result =
          realService().create(new MonthlyShiftInput(october, all, List.of()));

      Map<String, Integer> totals = new HashMap<>();
      for (DailyShiftResult day : result.days()) {
        for (EmployeeProfile profile : all) {
          totals.merge(
              profile.name() + mondayOf(day.date()),
              netMinutes(day, profile.name()),
              (a, b) -> a + b);
        }
      }
      assertTrue(totals.values().stream().allMatch(v -> v <= 1200));
      assertEquals(21, result.days().size());
    }

    @Test
    @DisplayName("[H-4][6章] Given: 8 名のパートだけの入力, When: create を実行すると, Then: 上限で不成立の日の理由が週上限になる")
    void weeklyLimitFailureHasWeeklyLimitReason() {
      MonthlyShiftResult result =
          realService()
              .create(
                  new MonthlyShiftInput(
                      october, profiles(EmploymentType.PART_TIME, "P", 8), List.of()));

      List<DailyShiftResult> failed =
          result.days().stream().filter(d -> d.assignment().isEmpty()).toList();
      assertTrue(!failed.isEmpty());
      assertTrue(
          failed.stream()
              .allMatch(d -> d.failureReason().equals(Optional.of(FailureReason.WEEKLY_LIMIT))));
    }

    @Test
    @DisplayName("[H-4] Given: 月初の 2 日だけの週と 5 日の週, When: create を実行すると, Then: 週は対象月の営業日だけで数えられる")
    void weeksAreCountedWithinTheMonth() {
      MonthlyShiftResult result =
          realService()
              .create(
                  new MonthlyShiftInput(
                      october, profiles(EmploymentType.PART_TIME, "P", 8), List.of()));

      long firstWeek =
          result.days().stream()
              .filter(
                  d -> d.date().isBefore(LocalDate.of(2026, 10, 5)) && d.assignment().isPresent())
              .count();
      long secondWeek =
          result.days().stream()
              .filter(
                  d ->
                      !d.date().isBefore(LocalDate.of(2026, 10, 5))
                          && d.date().isBefore(LocalDate.of(2026, 10, 12))
                          && d.assignment().isPresent())
              .count();
      assertEquals(2, firstWeek);
      assertEquals(2, secondWeek);
    }

    @Test
    @DisplayName("[H-4] Given: パートがいない入力, When: create を実行すると, Then: 各日の従来の assign と同じ結果になる")
    void sameAsPlainAssignWithoutParts() {
      List<EmployeeProfile> all = profiles(EmploymentType.FULL_TIME, "F", 9);
      MonthlyShiftResult result =
          realService().create(new MonthlyShiftInput(october, all, List.of()));

      ShiftAssignmentService plain = new ShiftAssignmentServiceImpl();
      List<Employee> employees = new ArrayList<>();
      for (EmployeeProfile profile : all) {
        employees.add(
            Employee.working(
                profile.name(),
                profile.employmentType(),
                LocalTime.of(7, 30),
                LocalTime.of(18, 30)));
      }
      for (DailyShiftResult day : result.days()) {
        assertEquals(plain.assign(employees), day.assignment());
      }
    }
  }

  private Optional<AssignmentResult> createDummyAssignmentResult() {
    List<ShiftAssignment> assignments = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      ShiftAssignment assignment = mock(ShiftAssignment.class);
      assignments.add(assignment);
    }
    AssignmentResult result = new AssignmentResult(assignments, 0, List.of());
    return Optional.of(result);
  }
}
