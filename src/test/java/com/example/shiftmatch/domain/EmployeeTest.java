package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Employee")
class EmployeeTest {

  @Nested
  @DisplayName("[F-1] 従業員入力（時間帯・休み）")
  class EmployeeTimeRange {

    @Test
    @DisplayName(
        "[F-1] Given: Employee.working()で従業員を作成するとき, When: 属性にアクセスすると, Then:"
            + " nameと時間帯が取得でき、offがfalseである")
    void workingEmployeeHasCorrectAttributes() {
      String name = "山田太郎";
      LocalTime start = LocalTime.of(8, 0);
      LocalTime end = LocalTime.of(17, 0);

      Employee employee = Employee.working(name, start, end);

      assertEquals(name, employee.name());
      assertEquals(start, employee.start());
      assertEquals(end, employee.end());
      assertEquals(false, employee.off());
    }

    @Test
    @DisplayName(
        "[F-1] Given: Employee.onLeave()で従業員を作成するとき, When: 属性にアクセスすると, Then:"
            + " offがtrueで、startとendがnullである")
    void onLeaveEmployeeHasNullTimeRange() {
      String name = "山田太郎";

      Employee employee = Employee.onLeave(name);

      assertEquals(name, employee.name());
      assertEquals(true, employee.off());
      assertNull(employee.start());
      assertNull(employee.end());
    }
  }

  @Nested
  @DisplayName("[H-3] 枠が入力時間帯に完全に含まれるかを判定")
  class CanWork {

    @Test
    @DisplayName(
        "[H-3] Given: 8:00〜17:00の従業員とき, When: canWorkを各枠で呼ぶと, Then:" + " 枠2・3・4に入れ、枠5・1には入れない")
    void canWorkWithinTimeRange() {
      Employee employee = Employee.working("太郎", LocalTime.of(8, 0), LocalTime.of(17, 0));

      assertEquals(false, employee.canWork(ShiftSlot.SLOT_1)); // 7:30開始 → 8:00より早い
      assertEquals(true, employee.canWork(ShiftSlot.SLOT_2)); // 8:00〜15:30 (完全に含まれる)
      assertEquals(true, employee.canWork(ShiftSlot.SLOT_3)); // 8:30〜16:30 (完全に含まれる)
      assertEquals(true, employee.canWork(ShiftSlot.SLOT_4)); // 9:00〜16:30 (完全に含まれる)
      assertEquals(false, employee.canWork(ShiftSlot.SLOT_5)); // 9:00〜18:00 (入力が17:00までなので終了が早い)
      assertEquals(false, employee.canWork(ShiftSlot.SLOT_6)); // 9:00〜18:30 (終了が18:30まで)
    }

    @Test
    @DisplayName("[H-3] Given: 入力の開始=枠の開始、入力の終了=枠の終了のとき, When: canWorkを呼ぶと, Then: trueである")
    void canWorkWhenBoundariesMatch() {
      Employee employee = Employee.working("太郎", LocalTime.of(8, 0), LocalTime.of(15, 30));

      assertEquals(true, employee.canWork(ShiftSlot.SLOT_2)); // 8:00〜15:30 (完全一致)
    }

    @Test
    @DisplayName("[H-3] Given: 休みの従業員のとき, When: canWorkを呼ぶと, Then: 全枠でfalseである")
    void canWorkReturnsFalseForOnLeaveEmployee() {
      Employee employee = Employee.onLeave("太郎");

      assertEquals(false, employee.canWork(ShiftSlot.SLOT_1));
      assertEquals(false, employee.canWork(ShiftSlot.SLOT_2));
      assertEquals(false, employee.canWork(ShiftSlot.SLOT_3));
      assertEquals(false, employee.canWork(ShiftSlot.SLOT_4));
      assertEquals(false, employee.canWork(ShiftSlot.SLOT_5));
      assertEquals(false, employee.canWork(ShiftSlot.SLOT_6));
    }

    @Test
    @DisplayName("[H-3] Given: startがnullの従業員のとき, When: canWorkを呼ぶと, Then: falseである")
    void canWorkReturnsFalseWhenStartIsNull() {
      Employee employee = new Employee("太郎", false, null, LocalTime.of(17, 0));

      assertEquals(false, employee.canWork(ShiftSlot.SLOT_1));
    }

    @Test
    @DisplayName("[H-3] Given: endがnullの従業員のとき, When: canWorkを呼ぶと, Then: falseである")
    void canWorkReturnsFalseWhenEndIsNull() {
      Employee employee = new Employee("太郎", false, LocalTime.of(8, 0), null);

      assertEquals(false, employee.canWork(ShiftSlot.SLOT_1));
    }
  }

  @Nested
  @DisplayName("[F-3] 「ずれ」（入力時間帯と枠の勤務時間の差）を計算")
  class GapMinutes {

    @Test
    @DisplayName(
        "[F-3] Given: 8:00〜17:00（540分）の従業員が枠2（450分）に入るとき, When: gapMinutesを呼ぶと," + " Then: 90が返る")
    void gapMinutesCalculatesCorrectly() {
      Employee employee = Employee.working("太郎", LocalTime.of(8, 0), LocalTime.of(17, 0));

      int gap = employee.gapMinutes(ShiftSlot.SLOT_2);

      assertEquals(90, gap);
    }

    @Test
    @DisplayName(
        "[F-3] Given: 入力時間帯と枠がちょうど一致（例：9:00〜18:30と枠6）のとき, When: gapMinutesを呼ぶと, Then:" + " 0が返る")
    void gapMinutesIsZeroWhenExactMatch() {
      Employee employee = Employee.working("太郎", LocalTime.of(9, 0), LocalTime.of(18, 30));

      int gap = employee.gapMinutes(ShiftSlot.SLOT_6);

      assertEquals(0, gap);
    }

    @Test
    @DisplayName(
        "[F-3] Given: 入れない枠を指定するとき, When: gapMinutesを呼ぶと, Then: IllegalStateExceptionがスローされる")
    void gapMinutesThrowsExceptionWhenCannotWork() {
      Employee employee = Employee.working("太郎", LocalTime.of(8, 0), LocalTime.of(17, 0));

      assertThrows(IllegalStateException.class, () -> employee.gapMinutes(ShiftSlot.SLOT_5));
    }
  }
}
