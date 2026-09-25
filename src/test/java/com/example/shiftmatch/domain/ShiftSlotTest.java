package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ShiftSlot")
class ShiftSlotTest {

  @Nested
  @DisplayName("[C-1][C-3] 枠の定義")
  class SlotDefinition {

    @Test
    @DisplayName(
        "[C-1][C-3] Given: ShiftSlot.SLOT_1を参照するとき, When: startTimeメソッドを呼ぶと, Then: 07:30が返る")
    void slot1StartsAt0730() {
      assertEquals(LocalTime.of(7, 30), ShiftSlot.SLOT_1.startTime());
    }

    @Test
    @DisplayName("[C-1][C-3] Given: ShiftSlot.SLOT_1を参照するとき, When: endTimeメソッドを呼ぶと, Then: 14:30が返る")
    void slot1EndsAt1430() {
      assertEquals(LocalTime.of(14, 30), ShiftSlot.SLOT_1.endTime());
    }

    @Test
    @DisplayName(
        "[C-1][C-3] Given: ShiftSlot.SLOT_1を参照するとき, When: numberOfEmployeesメソッドを呼ぶと, Then: 2が返る")
    void slot1HasTwoEmployees() {
      assertEquals(2, ShiftSlot.SLOT_1.numberOfEmployees());
    }

    @Test
    @DisplayName(
        "[C-1][C-3] Given: ShiftSlot.SLOT_2を参照するとき, When: startTimeメソッドを呼ぶと, Then: 08:00が返る")
    void slot2StartsAt0800() {
      assertEquals(LocalTime.of(8, 0), ShiftSlot.SLOT_2.startTime());
    }

    @Test
    @DisplayName("[C-1][C-3] Given: ShiftSlot.SLOT_2を参照するとき, When: endTimeメソッドを呼ぶと, Then: 15:30が返る")
    void slot2EndsAt1530() {
      assertEquals(LocalTime.of(15, 30), ShiftSlot.SLOT_2.endTime());
    }

    @Test
    @DisplayName(
        "[C-1][C-3] Given: ShiftSlot.SLOT_2を参照するとき, When: numberOfEmployeesメソッドを呼ぶと, Then: 1が返る")
    void slot2HasOneEmployee() {
      assertEquals(1, ShiftSlot.SLOT_2.numberOfEmployees());
    }

    @Test
    @DisplayName(
        "[C-1][C-3] Given: ShiftSlot.SLOT_3を参照するとき, When: startTimeメソッドを呼ぶと, Then: 08:30が返る")
    void slot3StartsAt0830() {
      assertEquals(LocalTime.of(8, 30), ShiftSlot.SLOT_3.startTime());
    }

    @Test
    @DisplayName("[C-1][C-3] Given: ShiftSlot.SLOT_3を参照するとき, When: endTimeメソッドを呼ぶと, Then: 16:30が返る")
    void slot3EndsAt1630() {
      assertEquals(LocalTime.of(16, 30), ShiftSlot.SLOT_3.endTime());
    }

    @Test
    @DisplayName(
        "[C-1][C-3] Given: ShiftSlot.SLOT_3を参照するとき, When: numberOfEmployeesメソッドを呼ぶと, Then: 1が返る")
    void slot3HasOneEmployee() {
      assertEquals(1, ShiftSlot.SLOT_3.numberOfEmployees());
    }

    @Test
    @DisplayName(
        "[C-1][C-3] Given: ShiftSlot.SLOT_4を参照するとき, When: startTimeメソッドを呼ぶと, Then: 09:00が返る")
    void slot4StartsAt0900() {
      assertEquals(LocalTime.of(9, 0), ShiftSlot.SLOT_4.startTime());
    }

    @Test
    @DisplayName("[C-1][C-3] Given: ShiftSlot.SLOT_4を参照するとき, When: endTimeメソッドを呼ぶと, Then: 16:30が返る")
    void slot4EndsAt1630() {
      assertEquals(LocalTime.of(16, 30), ShiftSlot.SLOT_4.endTime());
    }

    @Test
    @DisplayName(
        "[C-1][C-3] Given: ShiftSlot.SLOT_4を参照するとき, When: numberOfEmployeesメソッドを呼ぶと, Then: 1が返る")
    void slot4HasOneEmployee() {
      assertEquals(1, ShiftSlot.SLOT_4.numberOfEmployees());
    }

    @Test
    @DisplayName(
        "[C-1][C-3] Given: ShiftSlot.SLOT_5を参照するとき, When: startTimeメソッドを呼ぶと, Then: 09:00が返る")
    void slot5StartsAt0900() {
      assertEquals(LocalTime.of(9, 0), ShiftSlot.SLOT_5.startTime());
    }

    @Test
    @DisplayName("[C-1][C-3] Given: ShiftSlot.SLOT_5を参照するとき, When: endTimeメソッドを呼ぶと, Then: 18:00が返る")
    void slot5EndsAt1800() {
      assertEquals(LocalTime.of(18, 0), ShiftSlot.SLOT_5.endTime());
    }

    @Test
    @DisplayName(
        "[C-1][C-3] Given: ShiftSlot.SLOT_5を参照するとき, When: numberOfEmployeesメソッドを呼ぶと, Then: 1が返る")
    void slot5HasOneEmployee() {
      assertEquals(1, ShiftSlot.SLOT_5.numberOfEmployees());
    }

    @Test
    @DisplayName(
        "[C-1][C-3] Given: ShiftSlot.SLOT_6を参照するとき, When: startTimeメソッドを呼ぶと, Then: 09:00が返る")
    void slot6StartsAt0900() {
      assertEquals(LocalTime.of(9, 0), ShiftSlot.SLOT_6.startTime());
    }

    @Test
    @DisplayName("[C-1][C-3] Given: ShiftSlot.SLOT_6を参照するとき, When: endTimeメソッドを呼ぶと, Then: 18:30が返る")
    void slot6EndsAt1830() {
      assertEquals(LocalTime.of(18, 30), ShiftSlot.SLOT_6.endTime());
    }

    @Test
    @DisplayName(
        "[C-1][C-3] Given: ShiftSlot.SLOT_6を参照するとき, When: numberOfEmployeesメソッドを呼ぶと, Then: 2が返る")
    void slot6HasTwoEmployees() {
      assertEquals(2, ShiftSlot.SLOT_6.numberOfEmployees());
    }

    @Test
    @DisplayName("[C-1][C-3] Given: すべての枠を参照するとき, When: numberOfEmployeesを合計すると, Then: 8になる")
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
    @DisplayName(
        "[C-6] Given: ShiftSlot.SLOT_1を参照するとき, When: breakDurationMinutesメソッドを呼ぶと, Then: 45が返る")
    void slot1BreakDurationIs45Minutes() {
      assertEquals(45, ShiftSlot.SLOT_1.breakDurationMinutes());
    }

    @Test
    @DisplayName(
        "[C-6] Given: ShiftSlot.SLOT_2を参照するとき, When: breakDurationMinutesメソッドを呼ぶと, Then: 45が返る")
    void slot2BreakDurationIs45Minutes() {
      assertEquals(45, ShiftSlot.SLOT_2.breakDurationMinutes());
    }

    @Test
    @DisplayName(
        "[C-6] Given: ShiftSlot.SLOT_3を参照するとき, When: breakDurationMinutesメソッドを呼ぶと, Then: 45が返る")
    void slot3BreakDurationIs45Minutes() {
      assertEquals(45, ShiftSlot.SLOT_3.breakDurationMinutes());
    }

    @Test
    @DisplayName(
        "[C-6] Given: ShiftSlot.SLOT_4を参照するとき, When: breakDurationMinutesメソッドを呼ぶと, Then: 45が返る")
    void slot4BreakDurationIs45Minutes() {
      assertEquals(45, ShiftSlot.SLOT_4.breakDurationMinutes());
    }

    @Test
    @DisplayName(
        "[C-6] Given: ShiftSlot.SLOT_5を参照するとき, When: breakDurationMinutesメソッドを呼ぶと, Then: 60が返る")
    void slot5BreakDurationIs60Minutes() {
      assertEquals(60, ShiftSlot.SLOT_5.breakDurationMinutes());
    }

    @Test
    @DisplayName(
        "[C-6] Given: ShiftSlot.SLOT_6を参照するとき, When: breakDurationMinutesメソッドを呼ぶと, Then: 60が返る")
    void slot6BreakDurationIs60Minutes() {
      assertEquals(60, ShiftSlot.SLOT_6.breakDurationMinutes());
    }
  }
}
