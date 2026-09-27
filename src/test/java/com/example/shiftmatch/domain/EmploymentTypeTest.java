package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EmploymentType")
class EmploymentTypeTest {

  @Nested
  @DisplayName("[V-7] 雇用区分のラベルと解析")
  class EmploymentTypeValidation {

    @Test
    @DisplayName("[V-7] Given: FULL_TIME, When: label()を呼ぶと, Then: \"常勤\" を返す")
    void fullTimeReturnsCorrectLabel() {
      assertEquals("常勤", EmploymentType.FULL_TIME.label());
    }

    @Test
    @DisplayName("[V-7] Given: PART_TIME, When: label()を呼ぶと, Then: \"パート\" を返す")
    void partTimeReturnsCorrectLabel() {
      assertEquals("パート", EmploymentType.PART_TIME.label());
    }

    @Test
    @DisplayName("[V-7] Given: MANAGER, When: label()を呼ぶと, Then: \"管理職\" を返す")
    void managerReturnsCorrectLabel() {
      assertEquals("管理職", EmploymentType.MANAGER.label());
    }

    @Test
    @DisplayName("[V-7] Given: \"FULL_TIME\", When: parse()を呼ぶと, Then: FULL_TIME を返す")
    void parseFullTime() {
      Optional<EmploymentType> result = EmploymentType.parse("FULL_TIME");
      assertEquals(Optional.of(EmploymentType.FULL_TIME), result);
    }

    @Test
    @DisplayName("[V-7] Given: \"PART_TIME\", When: parse()を呼ぶと, Then: PART_TIME を返す")
    void parsePartTime() {
      Optional<EmploymentType> result = EmploymentType.parse("PART_TIME");
      assertEquals(Optional.of(EmploymentType.PART_TIME), result);
    }

    @Test
    @DisplayName("[V-7] Given: \"MANAGER\", When: parse()を呼ぶと, Then: MANAGER を返す")
    void parseManager() {
      Optional<EmploymentType> result = EmploymentType.parse("MANAGER");
      assertEquals(Optional.of(EmploymentType.MANAGER), result);
    }

    @Test
    @DisplayName("[V-7] Given: null, When: parse()を呼ぶと, Then: Optional.empty() を返す")
    void parseNull() {
      Optional<EmploymentType> result = EmploymentType.parse(null);
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("[V-7] Given: 空文字列, When: parse()を呼ぶと, Then: Optional.empty() を返す")
    void parseEmpty() {
      Optional<EmploymentType> result = EmploymentType.parse("");
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("[V-7] Given: \"EXECUTIVE\", When: parse()を呼ぶと, Then: Optional.empty() を返す")
    void parseInvalid() {
      Optional<EmploymentType> result = EmploymentType.parse("EXECUTIVE");
      assertTrue(result.isEmpty());
    }
  }

  @Nested
  @DisplayName("[H-4] 週の実働時間の上限")
  class WeeklyLimit {

    @Test
    @DisplayName("[H-4] Given: PART_TIME, When: hasWeeklyLimit()を呼ぶと, Then: true を返す")
    void partTimeHasWeeklyLimit() {
      assertTrue(EmploymentType.PART_TIME.hasWeeklyLimit());
    }

    @Test
    @DisplayName("[H-4] Given: FULL_TIME と MANAGER, When: hasWeeklyLimit()を呼ぶと, Then: false を返す")
    void fullTimeAndManagerHaveNoWeeklyLimit() {
      assertFalse(EmploymentType.FULL_TIME.hasWeeklyLimit());
      assertFalse(EmploymentType.MANAGER.hasWeeklyLimit());
    }

    @Test
    @DisplayName("[H-4] Given: 上限の定数, When: 参照すると, Then: 1200 分である")
    void partTimeWeeklyLimitIs1200Minutes() {
      assertEquals(1200, EmploymentType.PART_TIME_WEEKLY_LIMIT_MINUTES);
    }
  }
}
