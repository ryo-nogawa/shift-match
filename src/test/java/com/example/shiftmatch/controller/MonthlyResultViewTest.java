package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.shiftmatch.controller.MonthlyResultView.MonthlyHours;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MonthlyResultViewTest {

  private static MonthlyHours hours(int workDays, int totalMinutes) {
    return new MonthlyHours("n", "常勤", workDays, totalMinutes);
  }

  private static MonthlyResultView viewOf(List<MonthlyHours> rows) {
    return new MonthlyResultView(0, 0, 0, List.of(), List.of(), List.of(), rows);
  }

  @Nested
  class 時間の表記 {

    @Test
    @DisplayName("[F-4][7.1節] Given: 0 分, When: durationLabel, Then: (00:00)")
    void formatsZero() {
      assertEquals("(00:00)", hours(0, 0).durationLabel());
    }

    @Test
    @DisplayName("[F-4][7.1節] Given: 59 分, When: durationLabel, Then: (00:59)")
    void formatsMinutesOnly() {
      assertEquals("(00:59)", hours(1, 59).durationLabel());
    }

    @Test
    @DisplayName("[F-4][7.1節] Given: 60 分, When: durationLabel, Then: (01:00)")
    void formatsOneHour() {
      assertEquals("(01:00)", hours(1, 60).durationLabel());
    }

    @Test
    @DisplayName("[F-4][7.1節] Given: 885 分, When: durationLabel, Then: (14:45)")
    void formats885() {
      assertEquals("(14:45)", hours(2, 885).durationLabel());
    }

    @Test
    @DisplayName("[F-4][7.1節] Given: 7605 分, When: durationLabel, Then: 時間が 3 桁でも (126:45)")
    void formatsOverTwoDigitHours() {
      assertEquals("(126:45)", hours(20, 7605).durationLabel());
    }
  }

  @Nested
  class 合計行 {

    @Test
    @DisplayName("[F-4][7.1節] Given: 2 名の行, When: 合計を求めると, Then: 各人の合計と一致する")
    void sumsAllRows() {
      MonthlyResultView view = viewOf(List.of(hours(2, 885), hours(3, 1125)));

      assertEquals(5, view.totalWorkDays());
      assertEquals(2010, view.totalMinutes());
      assertEquals("(33:30)", view.totalDurationLabel());
    }

    @Test
    @DisplayName("[F-4][7.1節] Given: 従業員が 0 名, When: 合計を求めると, Then: 0 と (00:00)")
    void returnsZeroForNoEmployees() {
      MonthlyResultView view = viewOf(List.of());

      assertEquals(0, view.totalWorkDays());
      assertEquals(0, view.totalMinutes());
      assertEquals("(00:00)", view.totalDurationLabel());
    }
  }
}
