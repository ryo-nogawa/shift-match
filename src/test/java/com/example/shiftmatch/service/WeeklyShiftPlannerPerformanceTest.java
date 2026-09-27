package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.MonthlyShiftInput;
import com.example.shiftmatch.domain.MonthlyShiftResult;
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

@DisplayName("WeeklyShiftPlanner の性能")
class WeeklyShiftPlannerPerformanceTest {

  private static final long LIMIT_MILLIS = 10_000;

  @Test
  @DisplayName(
      "[9章][H-4] Given: 12 名全員パートで全員が終日入力, When: 2026 年 10 月分を create すると, Then: 数秒以内に終わり各週の実働合計が"
          + " 1200 分以内")
  void twelvePartsFinishWithinLimit() {
    YearMonth october = YearMonth.of(2026, 10);
    List<LocalDate> businessDays = new ArrayList<>();
    for (int d = 1; d <= 31; d++) {
      LocalDate date = LocalDate.of(2026, 10, d);
      boolean weekend =
          date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
      if (!weekend && d != 12) {
        businessDays.add(date);
      }
    }
    HolidayService holidayService = mock(HolidayService.class);
    when(holidayService.businessDays(october)).thenReturn(businessDays);
    MonthlyInputValidator validator = mock(MonthlyInputValidator.class);
    when(validator.validate(any())).thenReturn(new ArrayList<>());
    MonthlyShiftServiceImpl service =
        new MonthlyShiftServiceImpl(
            holidayService,
            new ShiftAssignmentServiceImpl(),
            validator,
            mock(SelectionRationaleLogger.class));

    Map<DayOfWeek, DailyWish> baseShifts = new HashMap<>();
    for (int i = 1; i <= 5; i++) {
      baseShifts.put(
          DayOfWeek.of(i), new DailyWish(false, LocalTime.of(7, 30), LocalTime.of(18, 30)));
    }
    List<EmployeeProfile> profiles = new ArrayList<>();
    for (int i = 0; i < 12; i++) {
      profiles.add(new EmployeeProfile("P" + i, EmploymentType.PART_TIME, baseShifts));
    }

    long started = System.nanoTime();
    MonthlyShiftResult result = service.create(new MonthlyShiftInput(october, profiles, List.of()));
    long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
    System.out.println("PERF 12 名全員パート 2026 年 10 月: " + elapsedMillis + " ms");

    assertTrue(elapsedMillis <= LIMIT_MILLIS, "実測 " + elapsedMillis + " ms");
    assertEquals(businessDays.size(), result.days().size());
    Map<String, Integer> totals = new HashMap<>();
    for (DailyShiftResult day : result.days()) {
      day.assignment()
          .ifPresent(
              assignment ->
                  assignment
                      .assignments()
                      .forEach(
                          a ->
                              totals.merge(
                                  a.employee().name() + day.date().with(DayOfWeek.MONDAY),
                                  a.slot().netWorkMinutes(),
                                  (x, y) -> x + y)));
    }
    assertTrue(totals.values().stream().allMatch(v -> v <= 1200));
  }
}
