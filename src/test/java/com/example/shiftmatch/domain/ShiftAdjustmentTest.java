package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ShiftAdjustment")
class ShiftAdjustmentTest {

  @Nested
  class 正常系 {

    @Test
    @DisplayName(
        "Given: 日付と従業員名と勤務時間帯を与えるとき, When: ShiftAdjustment を作成すると, Then: 作成されたオブジェクトが正しい値を持つ")
    void createsCorrectly() {
      LocalDate date = LocalDate.of(2024, 9, 2);
      LocalTime start = LocalTime.of(10, 0);
      LocalTime end = LocalTime.of(17, 0);
      DailyWish wish = new DailyWish(false, start, end);
      ShiftAdjustment adjustment = new ShiftAdjustment(date, "Taro", wish);

      assertEquals(date, adjustment.date());
      assertEquals("Taro", adjustment.employeeName());
      assertEquals(wish, adjustment.wish());
    }

    @Test
    @DisplayName(
        "Given: 日付と従業員名と休みを与えるとき, When: ShiftAdjustment を作成すると, Then: 作成されたオブジェクトに休みが設定される")
    void createsWithLeaveCorrectly() {
      LocalDate date = LocalDate.of(2024, 9, 2);
      DailyWish wish = new DailyWish(true, null, null);
      ShiftAdjustment adjustment = new ShiftAdjustment(date, "Taro", wish);

      assertEquals(date, adjustment.date());
      assertEquals("Taro", adjustment.employeeName());
      assertTrue(adjustment.wish().off());
    }
  }
}
