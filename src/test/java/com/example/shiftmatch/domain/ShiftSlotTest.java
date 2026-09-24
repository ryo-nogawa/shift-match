package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ShiftSlot")
class ShiftSlotTest {

  @Nested
  @DisplayName("[C-6] 枠の定義")
  class SlotDefinition {

    @Test
    @DisplayName("枠1の開始時刻が07:30であること")
    void slot1StartsAt0730() {
      assertEquals(LocalTime.of(7, 30), ShiftSlot.SLOT_1.startTime());
    }

    @Test
    @DisplayName("枠1の終了時刻が14:30であること")
    void slot1EndsAt1430() {
      assertEquals(LocalTime.of(14, 30), ShiftSlot.SLOT_1.endTime());
    }

    @Test
    @DisplayName("枠1の人数が2名であること")
    void slot1HasTwoEmployees() {
      assertEquals(2, ShiftSlot.SLOT_1.numberOfEmployees());
    }

    @Test
    @DisplayName("枠2の開始時刻が08:00であること")
    void slot2StartsAt0800() {
      assertEquals(LocalTime.of(8, 0), ShiftSlot.SLOT_2.startTime());
    }

    @Test
    @DisplayName("枠2の終了時刻が15:30であること")
    void slot2EndsAt1530() {
      assertEquals(LocalTime.of(15, 30), ShiftSlot.SLOT_2.endTime());
    }

    @Test
    @DisplayName("枠2の人数が1名であること")
    void slot2HasOneEmployee() {
      assertEquals(1, ShiftSlot.SLOT_2.numberOfEmployees());
    }

    @Test
    @DisplayName("枠3の開始時刻が08:30であること")
    void slot3StartsAt0830() {
      assertEquals(LocalTime.of(8, 30), ShiftSlot.SLOT_3.startTime());
    }

    @Test
    @DisplayName("枠3の終了時刻が16:30であること")
    void slot3EndsAt1630() {
      assertEquals(LocalTime.of(16, 30), ShiftSlot.SLOT_3.endTime());
    }

    @Test
    @DisplayName("枠3の人数が1名であること")
    void slot3HasOneEmployee() {
      assertEquals(1, ShiftSlot.SLOT_3.numberOfEmployees());
    }

    @Test
    @DisplayName("枠4の開始時刻が09:00であること")
    void slot4StartsAt0900() {
      assertEquals(LocalTime.of(9, 0), ShiftSlot.SLOT_4.startTime());
    }

    @Test
    @DisplayName("枠4の終了時刻が16:30であること")
    void slot4EndsAt1630() {
      assertEquals(LocalTime.of(16, 30), ShiftSlot.SLOT_4.endTime());
    }

    @Test
    @DisplayName("枠4の人数が1名であること")
    void slot4HasOneEmployee() {
      assertEquals(1, ShiftSlot.SLOT_4.numberOfEmployees());
    }

    @Test
    @DisplayName("枠5の開始時刻が09:00であること")
    void slot5StartsAt0900() {
      assertEquals(LocalTime.of(9, 0), ShiftSlot.SLOT_5.startTime());
    }

    @Test
    @DisplayName("枠5の終了時刻が18:00であること")
    void slot5EndsAt1800() {
      assertEquals(LocalTime.of(18, 0), ShiftSlot.SLOT_5.endTime());
    }

    @Test
    @DisplayName("枠5の人数が1名であること")
    void slot5HasOneEmployee() {
      assertEquals(1, ShiftSlot.SLOT_5.numberOfEmployees());
    }

    @Test
    @DisplayName("枠6の開始時刻が09:00であること")
    void slot6StartsAt0900() {
      assertEquals(LocalTime.of(9, 0), ShiftSlot.SLOT_6.startTime());
    }

    @Test
    @DisplayName("枠6の終了時刻が18:30であること")
    void slot6EndsAt1830() {
      assertEquals(LocalTime.of(18, 30), ShiftSlot.SLOT_6.endTime());
    }

    @Test
    @DisplayName("枠6の人数が2名であること")
    void slot6HasTwoEmployees() {
      assertEquals(2, ShiftSlot.SLOT_6.numberOfEmployees());
    }

    @Test
    @DisplayName("全枠の人数の合計が8名であること")
    void totalEmployeesAcrossAllSlotsIsEight() {
      int total = 0;
      for (ShiftSlot slot : ShiftSlot.values()) {
        total += slot.numberOfEmployees();
      }
      assertEquals(8, total);
    }
  }

  @Nested
  @DisplayName("[C-6] 休憩の長さ")
  class BreakDuration {

    @Test
    @DisplayName("枠1の休憩の長さが45分であること")
    void slot1BreakDurationIs45Minutes() {
      assertEquals(45, ShiftSlot.SLOT_1.breakDurationMinutes());
    }

    @Test
    @DisplayName("枠2の休憩の長さが45分であること")
    void slot2BreakDurationIs45Minutes() {
      assertEquals(45, ShiftSlot.SLOT_2.breakDurationMinutes());
    }

    @Test
    @DisplayName("枠3の休憩の長さが45分であること")
    void slot3BreakDurationIs45Minutes() {
      assertEquals(45, ShiftSlot.SLOT_3.breakDurationMinutes());
    }

    @Test
    @DisplayName("枠4の休憩の長さが45分であること")
    void slot4BreakDurationIs45Minutes() {
      assertEquals(45, ShiftSlot.SLOT_4.breakDurationMinutes());
    }

    @Test
    @DisplayName("枠5の休憩の長さが60分であること")
    void slot5BreakDurationIs60Minutes() {
      assertEquals(60, ShiftSlot.SLOT_5.breakDurationMinutes());
    }

    @Test
    @DisplayName("枠6の休憩の長さが60分であること")
    void slot6BreakDurationIs60Minutes() {
      assertEquals(60, ShiftSlot.SLOT_6.breakDurationMinutes());
    }
  }
}
