package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MonthlyShiftResult")
class MonthlyShiftResultTest {

  @Nested
  class 正常系 {

    @Test
    @DisplayName(
        "Given: 日次結果リストを与えるとき, When: MonthlyShiftResult を作成してから元のリストを変更すると, Then:"
            + " MonthlyShiftResult に変更が反映されない")
    void daysAreImmutable() {
      List<DailyShiftResult> days = new ArrayList<>();
      days.add(new DailyShiftResult(LocalDate.of(2024, 9, 2), 8, Optional.empty()));
      days.add(new DailyShiftResult(LocalDate.of(2024, 9, 3), 7, Optional.empty()));

      YearMonth month = YearMonth.of(2024, 9);
      MonthlyShiftResult result = new MonthlyShiftResult(month, days);

      // 元のリストを変更
      days.clear();

      // MonthlyShiftResult の日次結果リストは変わらない
      assertEquals(2, result.days().size());
      assertEquals(LocalDate.of(2024, 9, 2), result.days().get(0).date());
      assertEquals(LocalDate.of(2024, 9, 3), result.days().get(1).date());
    }
  }
}
