package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DailyShiftResult")
class DailyShiftResultTest {

  @Nested
  class 正常系 {

    @Test
    @DisplayName("Given: 割り当て結果があるとき, When: DailyShiftResult を作成すると, Then: 作成されたオブジェクトが正しい値を持つ")
    void createsWithAssignmentCorrectly() {
      LocalDate date = LocalDate.of(2024, 9, 2);
      // ダミーの AssignmentResult を作成
      AssignmentResult assignmentResult =
          new AssignmentResult(
              java.util.List.of(
                  new ShiftAssignment(
                      new Employee("Taro", EmploymentType.FULL_TIME, false, null, null),
                      ShiftSlot.SLOT_1,
                      java.time.LocalTime.of(12, 0),
                      java.time.LocalTime.of(12, 45)),
                  new ShiftAssignment(
                      new Employee("Hanako", EmploymentType.FULL_TIME, false, null, null),
                      ShiftSlot.SLOT_1,
                      java.time.LocalTime.of(12, 0),
                      java.time.LocalTime.of(12, 45)),
                  new ShiftAssignment(
                      new Employee("Jiro", EmploymentType.FULL_TIME, false, null, null),
                      ShiftSlot.SLOT_2,
                      java.time.LocalTime.of(12, 45),
                      java.time.LocalTime.of(13, 30)),
                  new ShiftAssignment(
                      new Employee("Sakura", EmploymentType.FULL_TIME, false, null, null),
                      ShiftSlot.SLOT_3,
                      java.time.LocalTime.of(12, 45),
                      java.time.LocalTime.of(13, 30)),
                  new ShiftAssignment(
                      new Employee("Takeshi", EmploymentType.FULL_TIME, false, null, null),
                      ShiftSlot.SLOT_4,
                      java.time.LocalTime.of(13, 30),
                      java.time.LocalTime.of(14, 15)),
                  new ShiftAssignment(
                      new Employee("Yuki", EmploymentType.FULL_TIME, false, null, null),
                      ShiftSlot.SLOT_5,
                      java.time.LocalTime.of(13, 30),
                      java.time.LocalTime.of(14, 30)),
                  new ShiftAssignment(
                      new Employee("Keiko", EmploymentType.FULL_TIME, false, null, null),
                      ShiftSlot.SLOT_6,
                      java.time.LocalTime.of(14, 15),
                      java.time.LocalTime.of(15, 15)),
                  new ShiftAssignment(
                      new Employee("Masao", EmploymentType.FULL_TIME, false, null, null),
                      ShiftSlot.SLOT_6,
                      java.time.LocalTime.of(14, 30),
                      java.time.LocalTime.of(15, 30))),
              0,
              java.util.List.of());

      DailyShiftResult result = new DailyShiftResult(date, 8, Optional.of(assignmentResult));

      assertEquals(date, result.date());
      assertEquals(8, result.availableCount());
      assertTrue(result.assignment().isPresent());
      assertEquals(assignmentResult, result.assignment().get());
    }

    @Test
    @DisplayName("Given: 割り当て結果がないとき, When: DailyShiftResult を作成すると, Then: 作成されたオブジェクトが正しい値を持つ")
    void createsWithoutAssignmentCorrectly() {
      LocalDate date = LocalDate.of(2024, 9, 2);
      DailyShiftResult result = new DailyShiftResult(date, 7, Optional.empty());

      assertEquals(date, result.date());
      assertEquals(7, result.availableCount());
      assertTrue(result.assignment().isEmpty());
    }
  }
}
