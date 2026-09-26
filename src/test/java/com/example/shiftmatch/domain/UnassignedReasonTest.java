package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("UnassignedReason")
class UnassignedReasonTest {

  @Nested
  @DisplayName("[F-4] 未出勤の理由を表示")
  class Label {

    @Test
    @DisplayName("[F-4] Given: ON_LEAVE, When: label()を呼ぶと, Then: \"休み\" を返す")
    void onLeaveReturnsCorrectLabel() {
      assertEquals("休み", UnassignedReason.ON_LEAVE.label());
    }

    @Test
    @DisplayName(
        "[F-4] Given: LOWER_GAP_CHOSEN, When: label()を呼ぶと, Then:"
            + " \"入れる枠はあったが、より小さいずれの案が選ばれた\" を返す")
    void lowerGapChosenReturnsCorrectLabel() {
      assertEquals("入れる枠はあったが、より小さいずれの案が選ばれた", UnassignedReason.LOWER_GAP_CHOSEN.label());
    }

    @Test
    @DisplayName("[F-4] Given: NO_AVAILABLE_SLOT, When: label()を呼ぶと, Then:" + " \"どの枠にも入れない\" を返す")
    void noAvailableSlotReturnsCorrectLabel() {
      assertEquals("どの枠にも入れない", UnassignedReason.NO_AVAILABLE_SLOT.label());
    }
  }
}
