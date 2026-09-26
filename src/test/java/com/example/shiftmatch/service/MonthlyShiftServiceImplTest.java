package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.EmploymentType;
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

    MonthlyShiftServiceImpl service =
        new MonthlyShiftServiceImpl(holidayService, assignmentService);

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

    MonthlyShiftServiceImpl service =
        new MonthlyShiftServiceImpl(holidayService, assignmentService);

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

    MonthlyShiftServiceImpl service =
        new MonthlyShiftServiceImpl(holidayService, assignmentService);

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
}
