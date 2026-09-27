package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
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

  @Nested
  class 不成立の理由 {

    private final LocalDate date = LocalDate.of(2026, 10, 1);

    private AssignmentResult anyAssignment() {
      List<ShiftAssignment> list = new ArrayList<>();
      for (ShiftSlot slot : ShiftSlot.values()) {
        for (int i = 0; i < slot.numberOfEmployees(); i++) {
          list.add(
              new ShiftAssignment(
                  Employee.working("e" + list.size(), slot.startTime(), slot.endTime()),
                  slot,
                  LocalTime.of(12, 0),
                  LocalTime.of(12, 45)));
        }
      }
      return new AssignmentResult(list, 0, List.of());
    }

    @Test
    @DisplayName("[6章] Given: 成立なのに理由がある, When: 作成すると, Then: IllegalArgumentException")
    void rejectsReasonWhenAssigned() {
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new DailyShiftResult(
                  date, 8, Optional.of(anyAssignment()), Optional.of(FailureReason.SHORTAGE)));
    }

    @Test
    @DisplayName("[6章] Given: 不成立なのに理由がない, When: 作成すると, Then: IllegalArgumentException")
    void rejectsMissingReasonWhenNotAssigned() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new DailyShiftResult(date, 3, Optional.empty(), Optional.empty()));
    }

    @Test
    @DisplayName("[6章] Given: 成立で理由なし, When: 作成すると, Then: 作成できる")
    void acceptsAssignedWithoutReason() {
      DailyShiftResult result =
          new DailyShiftResult(date, 8, Optional.of(anyAssignment()), Optional.empty());
      assertTrue(result.failureReason().isEmpty());
    }

    @Test
    @DisplayName("[6章] Given: 不成立で理由あり, When: 作成すると, Then: 理由を保持する")
    void keepsReasonWhenNotAssigned() {
      DailyShiftResult result =
          new DailyShiftResult(date, 9, Optional.empty(), Optional.of(FailureReason.WEEKLY_LIMIT));
      assertEquals(Optional.of(FailureReason.WEEKLY_LIMIT), result.failureReason());
    }

    @Test
    @DisplayName("[6章] Given: 理由を省いた 3 引数の作成, When: 不成立で作成すると, Then: 人員不足になる")
    void shortHandConstructorMeansShortage() {
      DailyShiftResult result = new DailyShiftResult(date, 3, Optional.empty());
      assertEquals(Optional.of(FailureReason.SHORTAGE), result.failureReason());
    }

    @Test
    @DisplayName("[6章] Given: 各理由, When: label()を呼ぶと, Then: 人員不足・パートの週上限")
    void labels() {
      assertEquals("人員不足", FailureReason.SHORTAGE.label());
      assertEquals("パートの週上限", FailureReason.WEEKLY_LIMIT.label());
    }
  }
}
