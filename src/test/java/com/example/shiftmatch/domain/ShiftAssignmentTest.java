package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ShiftAssignment")
class ShiftAssignmentTest {

  @Nested
  @DisplayName("[F-3] 割り当て 1 件分のずれを計算")
  class GapMinutes {

    @Test
    @DisplayName(
        "[F-3] Given: 8:00〜17:00の従業員が枠2に割り当てられるとき, When: gapMinutes()を呼ぶと, Then:" + " 90を返す")
    void gapMinutesCalculatesCorrectly() {
      Employee employee = Employee.working("太郎", LocalTime.of(8, 0), LocalTime.of(17, 0));
      ShiftAssignment assignment =
          new ShiftAssignment(
              employee, ShiftSlot.SLOT_2, LocalTime.of(12, 0), LocalTime.of(12, 45));

      int gap = assignment.gapMinutes();

      assertEquals(90, gap);
    }
  }
}
