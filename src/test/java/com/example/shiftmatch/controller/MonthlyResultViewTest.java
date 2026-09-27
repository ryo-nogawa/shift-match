package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.shiftmatch.controller.MonthlyResultView.CalendarDay;
import com.example.shiftmatch.controller.MonthlyResultView.EmployeeRow;
import com.example.shiftmatch.domain.FailureReason;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MonthlyResultViewTest {

  private static EmployeeRow row(int workDays, int totalMinutes) {
    return new EmployeeRow("n", List.of(), workDays, totalMinutes);
  }

  @Nested
  class 時間の表記 {

    @Test
    @DisplayName("[F-4][7.1節] Given: 0 分, When: durationLabel, Then: (00:00)")
    void formatsZero() {
      assertEquals("(00:00)", row(0, 0).durationLabel());
    }

    @Test
    @DisplayName("[F-4][7.1節] Given: 59 分, When: durationLabel, Then: (00:59)")
    void formatsMinutesOnly() {
      assertEquals("(00:59)", row(1, 59).durationLabel());
    }

    @Test
    @DisplayName("[F-4][7.1節] Given: 60 分, When: durationLabel, Then: (01:00)")
    void formatsOneHour() {
      assertEquals("(01:00)", row(1, 60).durationLabel());
    }

    @Test
    @DisplayName("[F-4][7.1節] Given: 885 分, When: durationLabel, Then: (14:45)")
    void formats885() {
      assertEquals("(14:45)", row(2, 885).durationLabel());
    }

    @Test
    @DisplayName("[F-4][7.1節] Given: 7605 分, When: durationLabel, Then: 時間が 3 桁でも (126:45)")
    void formatsOverTwoDigitHours() {
      assertEquals("(126:45)", row(20, 7605).durationLabel());
    }
  }

  @Nested
  @DisplayName("[7.1] 不成立の理由の文言")
  class CalendarDayFailureText {

    @Test
    @DisplayName("[7.1] Given: 理由がSTAFF_SHORTAGEで勤務可9名, When: failureText, Then: 不成立（勤務可 9 名）")
    void formatsStaffShortage() {
      CalendarDay day =
          new CalendarDay(
              LocalDate.of(2024, 9, 2), null, true, 9, List.of(), FailureReason.STAFF_SHORTAGE);

      assertEquals("不成立（勤務可 9 名）", day.failureText());
    }

    @Test
    @DisplayName("[7.1] Given: 理由がWEEKLY_LIMIT, When: failureText, Then: 不成立（パートの週上限）")
    void formatsWeeklyLimit() {
      CalendarDay day =
          new CalendarDay(
              LocalDate.of(2024, 9, 2), null, true, 7, List.of(), FailureReason.WEEKLY_LIMIT);

      assertEquals("不成立（パートの週上限）", day.failureText());
    }
  }
}
