package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("FailureReason")
class FailureReasonTest {

  @Nested
  @DisplayName("[6章] 不成立の理由を表示")
  class Label {

    @Test
    @DisplayName("[6章] Given: STAFF_SHORTAGE, When: label()を呼ぶと, Then: \"人員不足\" を返す")
    void staffShortageReturnsCorrectLabel() {
      assertEquals("人員不足", FailureReason.STAFF_SHORTAGE.label());
    }

    @Test
    @DisplayName("[6章] Given: WEEKLY_LIMIT, When: label()を呼ぶと, Then: \"パートの週上限\" を返す")
    void weeklyLimitReturnsCorrectLabel() {
      assertEquals("パートの週上限", FailureReason.WEEKLY_LIMIT.label());
    }
  }
}
