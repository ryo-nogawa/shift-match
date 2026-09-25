package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BreakScheduler")
class BreakSchedulerTest {

  @Nested
  @DisplayName("[C-6] 標準構成での休憩時刻割り当て")
  class StandardConfiguration {

    @Test
    @DisplayName(
        "[C-6] Given: 標準構成（枠1×2、枠2、枠3、枠4、枠5、枠6×2）が与えられたとき, When: 休憩時刻を割り当てると, Then: 仕様書の表と一致する")
    void assignsBreakTimesAccordingToSpecification() {
      List<ShiftSlot> slots = new ArrayList<>();
      slots.add(ShiftSlot.SLOT_1);
      slots.add(ShiftSlot.SLOT_1);
      slots.add(ShiftSlot.SLOT_2);
      slots.add(ShiftSlot.SLOT_3);
      slots.add(ShiftSlot.SLOT_4);
      slots.add(ShiftSlot.SLOT_5);
      slots.add(ShiftSlot.SLOT_6);
      slots.add(ShiftSlot.SLOT_6);

      BreakScheduler scheduler = new BreakScheduler();
      List<BreakInterval> breaks = scheduler.schedule(slots);

      assertEquals(8, breaks.size());

      assertEquals(LocalTime.of(12, 0), breaks.get(0).startTime());
      assertEquals(LocalTime.of(12, 45), breaks.get(0).endTime());

      assertEquals(LocalTime.of(12, 0), breaks.get(1).startTime());
      assertEquals(LocalTime.of(12, 45), breaks.get(1).endTime());

      assertEquals(LocalTime.of(12, 45), breaks.get(2).startTime());
      assertEquals(LocalTime.of(13, 30), breaks.get(2).endTime());

      assertEquals(LocalTime.of(12, 45), breaks.get(3).startTime());
      assertEquals(LocalTime.of(13, 30), breaks.get(3).endTime());

      assertEquals(LocalTime.of(13, 30), breaks.get(4).startTime());
      assertEquals(LocalTime.of(14, 15), breaks.get(4).endTime());

      assertEquals(LocalTime.of(13, 30), breaks.get(5).startTime());
      assertEquals(LocalTime.of(14, 30), breaks.get(5).endTime());

      assertEquals(LocalTime.of(14, 15), breaks.get(6).startTime());
      assertEquals(LocalTime.of(15, 15), breaks.get(6).endTime());

      assertEquals(LocalTime.of(14, 30), breaks.get(7).startTime());
      assertEquals(LocalTime.of(15, 30), breaks.get(7).endTime());
    }
  }

  @Nested
  @DisplayName("[C-6] 同時休憩の制限")
  class SimultaneousBreakLimit {

    @Test
    @DisplayName("[C-6] Given: 標準構成が与えられたとき, When: 休憩時刻を割り当てると, Then: どの時刻でも同時休憩は2名以下である")
    void noMoreThanTwoEmployeesOnBreakAtSameTime() {
      List<ShiftSlot> slots = new ArrayList<>();
      slots.add(ShiftSlot.SLOT_1);
      slots.add(ShiftSlot.SLOT_1);
      slots.add(ShiftSlot.SLOT_2);
      slots.add(ShiftSlot.SLOT_3);
      slots.add(ShiftSlot.SLOT_4);
      slots.add(ShiftSlot.SLOT_5);
      slots.add(ShiftSlot.SLOT_6);
      slots.add(ShiftSlot.SLOT_6);

      BreakScheduler scheduler = new BreakScheduler();
      List<BreakInterval> breaks = scheduler.schedule(slots);

      for (int hour = 12; hour <= 15; hour++) {
        for (int minute = 0; minute < 60; minute += 5) {
          LocalTime time = LocalTime.of(hour, minute);
          int breakCount = 0;
          for (BreakInterval breakTime : breaks) {
            if (!time.isBefore(breakTime.startTime()) && time.isBefore(breakTime.endTime())) {
              breakCount++;
            }
          }
          assertTrue(
              breakCount <= 2, "More than 2 employees on break at " + time + ": " + breakCount);
        }
      }
    }
  }

  @Nested
  @DisplayName("[C-6] 休憩時間の有効性")
  class BreakValidity {

    @Test
    @DisplayName("[C-6] Given: 標準構成が与えられたとき, When: 休憩時刻を割り当てると, Then: 各人の休憩は勤務時間内に収まる")
    void breaksAreWithinWorkingHours() {
      List<ShiftSlot> slots = new ArrayList<>();
      slots.add(ShiftSlot.SLOT_1);
      slots.add(ShiftSlot.SLOT_1);
      slots.add(ShiftSlot.SLOT_2);
      slots.add(ShiftSlot.SLOT_3);
      slots.add(ShiftSlot.SLOT_4);
      slots.add(ShiftSlot.SLOT_5);
      slots.add(ShiftSlot.SLOT_6);
      slots.add(ShiftSlot.SLOT_6);

      BreakScheduler scheduler = new BreakScheduler();
      List<BreakInterval> breaks = scheduler.schedule(slots);

      for (int i = 0; i < breaks.size(); i++) {
        ShiftSlot slot = slots.get(i);
        BreakInterval breakTime = breaks.get(i);

        assertTrue(
            !breakTime.startTime().isBefore(slot.startTime()),
            "Person " + i + " break starts before their shift");
        assertTrue(
            !breakTime.endTime().isAfter(slot.endTime()),
            "Person " + i + " break ends after their shift");
      }
    }

    @Test
    @DisplayName("[C-6] Given: 標準構成が与えられたとき, When: 休憩時刻を割り当てると, Then: 各人の休憩の長さが正しい")
    void breakDurationMatchesSlotRequirement() {
      List<ShiftSlot> slots = new ArrayList<>();
      slots.add(ShiftSlot.SLOT_1);
      slots.add(ShiftSlot.SLOT_1);
      slots.add(ShiftSlot.SLOT_2);
      slots.add(ShiftSlot.SLOT_3);
      slots.add(ShiftSlot.SLOT_4);
      slots.add(ShiftSlot.SLOT_5);
      slots.add(ShiftSlot.SLOT_6);
      slots.add(ShiftSlot.SLOT_6);

      BreakScheduler scheduler = new BreakScheduler();
      List<BreakInterval> breaks = scheduler.schedule(slots);

      for (int i = 0; i < breaks.size(); i++) {
        ShiftSlot slot = slots.get(i);
        BreakInterval breakTime = breaks.get(i);

        int expectedMinutes = slot.breakDurationMinutes();
        int actualMinutes =
            breakTime.endTime().getHour() * 60
                + breakTime.endTime().getMinute()
                - (breakTime.startTime().getHour() * 60 + breakTime.startTime().getMinute());

        assertEquals(expectedMinutes, actualMinutes, "Person " + i + " break duration mismatch");
      }
    }
  }
}
