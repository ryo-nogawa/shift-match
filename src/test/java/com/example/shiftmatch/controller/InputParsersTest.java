package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * InputParsers のテスト。
 */
@DisplayName("InputParsers")
class InputParsersTest {

  @Nested
  @DisplayName("正常系")
  class Valid {

    @Test
    @DisplayName("[V-8] Given: YYYY-MM, When: 変換すると, Then: 年月になる")
    void parsesYearMonth() {
      assertEquals(Optional.of(YearMonth.of(2026, 10)), InputParsers.parseYearMonth("2026-10"));
    }

    @Test
    @DisplayName("[V-9] Given: YYYY-MM-DD, When: 変換すると, Then: 日付になる")
    void parsesDate() {
      assertEquals(Optional.of(LocalDate.of(2026, 10, 20)), InputParsers.parseDate("2026-10-20"));
    }

    @Test
    @DisplayName("[V-3] Given: HH:mm, When: 変換すると, Then: 時刻になる")
    void parsesTime() {
      assertEquals(Optional.of(LocalTime.of(7, 30)), InputParsers.parseTime("07:30"));
    }
  }

  @Nested
  @DisplayName("異常系")
  class Invalid {

    @Test
    @DisplayName("[V-8] Given: 不正な年月, When: 変換すると, Then: 空になる")
    void rejectsInvalidYearMonth() {
      for (String text :
          new String[] {null, "", "2026-13", "2026-00", "2026-1", "abc", "2026-10-01"}) {
        assertTrue(InputParsers.parseYearMonth(text).isEmpty(), text);
      }
    }

    @Test
    @DisplayName("[V-9] Given: 不正な日付, When: 変換すると, Then: 空になる")
    void rejectsInvalidDate() {
      for (String text : new String[] {null, "", "2026-02-30", "2026-10-32", "2026-10-1", "abc"}) {
        assertTrue(InputParsers.parseDate(text).isEmpty(), text);
      }
    }

    @Test
    @DisplayName("[V-3] Given: 不正な時刻, When: 変換すると, Then: 空になる")
    void rejectsInvalidTime() {
      for (String text : new String[] {null, "", "24:00", "07:60", "7:30", "07:30:15", "abc"}) {
        assertTrue(InputParsers.parseTime(text).isEmpty(), text);
      }
    }
  }
}
