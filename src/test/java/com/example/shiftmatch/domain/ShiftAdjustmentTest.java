package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ShiftAdjustment")
class ShiftAdjustmentTest {

  @Test
  @DisplayName("個別変更を作成できる")
  void testCreate() {
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
  @DisplayName("個別変更で休みを指定できる")
  void testCreateWithLeave() {
    LocalDate date = LocalDate.of(2024, 9, 2);
    DailyWish wish = new DailyWish(true, null, null);
    ShiftAdjustment adjustment = new ShiftAdjustment(date, "Taro", wish);

    assertEquals(date, adjustment.date());
    assertEquals("Taro", adjustment.employeeName());
    assertTrue(adjustment.wish().off());
  }
}
