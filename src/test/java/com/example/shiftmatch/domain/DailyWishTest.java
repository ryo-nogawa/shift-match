package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DailyWish")
class DailyWishTest {

  @Test
  @DisplayName("休みの希望を作成できる")
  void testCreateOffWish() {
    DailyWish wish = new DailyWish(true, null, null);
    assertTrue(wish.off());
    assertNull(wish.start());
    assertNull(wish.end());
  }

  @Test
  @DisplayName("勤務時間帯を指定して作成できる")
  void testCreateWorkingWish() {
    LocalTime start = LocalTime.of(9, 0);
    LocalTime end = LocalTime.of(18, 0);
    DailyWish wish = new DailyWish(false, start, end);
    assertFalse(wish.off());
    assertEquals(start, wish.start());
    assertEquals(end, wish.end());
  }
}
