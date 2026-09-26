package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.InvalidMonthlyInputException;
import com.example.shiftmatch.domain.MonthlyShiftInput;
import com.example.shiftmatch.domain.MonthlyShiftResult;
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

@DisplayName("[F-3] 営業日ごとの独立した割り当て算出")
class MonthlyShiftServiceImplTest {

  @Test
  @DisplayName("[F-3] businessDays が返した日だけが結果に含まれ、順序も同じ")
  void testOnlyBusinessDaysInResult() {
    HolidayService holidayService = mock(HolidayService.class);
    ShiftAssignmentService assignmentService = mock(ShiftAssignmentService.class);

    LocalDate day1 = LocalDate.of(2024, 9, 2); // Monday
    LocalDate day2 = LocalDate.of(2024, 9, 3); // Tuesday
    YearMonth month = YearMonth.of(2024, 9);

    when(holidayService.businessDays(month)).thenReturn(List.of(day1, day2));
    when(holidayService.isSupported(month)).thenReturn(true);

    SelectionRationaleLogger logger = mock(SelectionRationaleLogger.class);
    MonthlyShiftServiceImpl service =
        new MonthlyShiftServiceImpl(holidayService, assignmentService, logger);

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
  @DisplayName("[V-1] 従業員名が空の行が結果に含まれない")
  void testEmptyNameExcluded() {
    HolidayService holidayService = mock(HolidayService.class);
    ShiftAssignmentService assignmentService = mock(ShiftAssignmentService.class);

    LocalDate day1 = LocalDate.of(2024, 9, 2);
    YearMonth month = YearMonth.of(2024, 9);

    when(holidayService.businessDays(month)).thenReturn(List.of(day1));
    when(holidayService.isSupported(month)).thenReturn(true);

    SelectionRationaleLogger logger = mock(SelectionRationaleLogger.class);
    MonthlyShiftServiceImpl service =
        new MonthlyShiftServiceImpl(holidayService, assignmentService, logger);

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

  @Test
  @DisplayName("[V-4] 勤務できる人が 8 名未満で不成立")
  void testUnsuccessfulWhenFewerThan8Available() {
    HolidayService holidayService = mock(HolidayService.class);
    ShiftAssignmentService assignmentService = mock(ShiftAssignmentService.class);

    LocalDate day1 = LocalDate.of(2024, 9, 2);
    LocalDate day2 = LocalDate.of(2024, 9, 3);
    YearMonth month = YearMonth.of(2024, 9);

    when(holidayService.businessDays(month)).thenReturn(List.of(day1, day2));
    when(holidayService.isSupported(month)).thenReturn(true);

    SelectionRationaleLogger logger = mock(SelectionRationaleLogger.class);
    MonthlyShiftServiceImpl service =
        new MonthlyShiftServiceImpl(holidayService, assignmentService, logger);

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

    // 火曜日（day2）だけ休みに変更
    LocalDate tuesdayDate = LocalDate.of(2024, 9, 3);
    DailyWish offWish = new DailyWish(true, null, null);
    ShiftAdjustment adjustment = new ShiftAdjustment(tuesdayDate, "Taro", offWish);

    MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), List.of(adjustment));

    MonthlyShiftResult result = service.create(input);

    assertEquals(2, result.days().size());
    DailyShiftResult day1Result = result.days().get(0);
    DailyShiftResult day2Result = result.days().get(1);

    // 月曜日は1人が勤務可能
    assertEquals(1, day1Result.availableCount());

    // 火曜日は0人が勤務可能
    assertEquals(0, day2Result.availableCount());
  }

  @Test
  @DisplayName("[V-3][V-9] 検証エラーで InvalidMonthlyInputException を投げ assign を呼ばない")
  void testValidationErrorThrowsException() {
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
    SelectionRationaleLogger logger = mock(SelectionRationaleLogger.class);
    MonthlyShiftServiceImpl service =
        new MonthlyShiftServiceImpl(holidayService, assignmentService, logger);
    assertThrows(InvalidMonthlyInputException.class, () -> service.create(input));

    // verify assign was never called
    verify(assignmentService, never()).assign(java.util.List.of());
  }

  @Test
  @DisplayName("[V-3][V-9] V-9エラーで InvalidMonthlyInputException を投げ assign を呼ばない")
  void testV9ValidationErrorThrowsException() {
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
    SelectionRationaleLogger logger = mock(SelectionRationaleLogger.class);
    MonthlyShiftServiceImpl service =
        new MonthlyShiftServiceImpl(holidayService, assignmentService, logger);

    assertThrows(InvalidMonthlyInputException.class, () -> service.create(input));

    // verify assign was never called
    verify(assignmentService, never()).assign(java.util.List.of());
  }

  @Test
  @DisplayName("[7.2 節] create が営業日ごとにログを出力する")
  void testCreateLogsForEachBusinessDay() {
    LocalDate day1 = LocalDate.of(2024, 9, 2); // Monday
    LocalDate day2 = LocalDate.of(2024, 9, 3); // Tuesday
    YearMonth month = YearMonth.of(2024, 9);

    HolidayService holidayService = mock(HolidayService.class);
    when(holidayService.businessDays(month)).thenReturn(List.of(day1, day2));
    when(holidayService.isSupported(month)).thenReturn(true);

    ShiftAssignmentService assignmentService = mock(ShiftAssignmentService.class);
    when(assignmentService.assign(java.util.List.of())).thenReturn(java.util.Optional.empty());

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
        new MonthlyShiftServiceImpl(holidayService, assignmentService, rationaleLogger);
    service.create(input);

    // verify logger.log was called for each business day
    verify(rationaleLogger).log(eq(day1), any());
    verify(rationaleLogger).log(eq(day2), any());
  }
}
