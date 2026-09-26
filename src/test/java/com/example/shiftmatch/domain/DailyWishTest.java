package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DailyWish")
class DailyWishTest {

  @Nested
  class 正常系 {

    @Test
    @DisplayName("Given: 休みのとき, When: DailyWish を作成すると, Then: 休みフラグと時刻が正しく設定される")
    void createsOffWishCorrectly() {
      DailyWish wish = new DailyWish(true, null, null);
      assertTrue(wish.off());
      assertNull(wish.start());
      assertNull(wish.end());
    }

    @Test
    @DisplayName("Given: 勤務時間帯を指定するとき, When: DailyWish を作成すると, Then: 勤務フラグと時刻が正しく設定される")
    void createsWorkingWishCorrectly() {
      LocalTime start = LocalTime.of(9, 0);
      LocalTime end = LocalTime.of(18, 0);
      DailyWish wish = new DailyWish(false, start, end);
      assertFalse(wish.off());
      assertEquals(start, wish.start());
      assertEquals(end, wish.end());
    }
  }
}
