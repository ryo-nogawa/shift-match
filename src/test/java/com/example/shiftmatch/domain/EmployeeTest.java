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
  @DisplayName("[F-1] 従業員入力（時間帯・休み・雇用区分）")
  class EmployeeTimeRange {

    @Test
    @DisplayName(
        "[F-1] Given: Employee.working()で従業員を作成するとき, When: 属性にアクセスすると, Then:"
            + " nameと時間帯が取得でき、offがfalseで、employmentTypeが常勤である")
    void workingEmployeeHasCorrectAttributes() {
      String name = "山田太郎";
      LocalTime start = LocalTime.of(8, 0);
      LocalTime end = LocalTime.of(17, 0);

      Employee employee = Employee.working(name, start, end);

      assertEquals(name, employee.name());
      assertEquals(start, employee.start());
      assertEquals(end, employee.end());
      assertEquals(false, employee.off());
      assertEquals(EmploymentType.FULL_TIME, employee.employmentType());
    }

    @Test
    @DisplayName(
        "[F-1] Given: Employee.onLeave()で従業員を作成するとき, When: 属性にアクセスすると, Then:"
            + " offがtrueで、startとendがnullで、employmentTypeが常勤である")
    void onLeaveEmployeeHasNullTimeRange() {
      String name = "山田太郎";

      Employee employee = Employee.onLeave(name);

      assertEquals(name, employee.name());
      assertEquals(true, employee.off());
      assertNull(employee.start());
      assertNull(employee.end());
      assertEquals(EmploymentType.FULL_TIME, employee.employmentType());
    }

    @Test
    @DisplayName(
        "[F-1] Given: Employee.working(name, type, start, end)で従業員を作成するとき, When: 属性にアクセスすると,"
            + " Then: employmentTypeに指定した型が保持される")
    void workingEmployeeWithSpecifiedType() {
      String name = "太郎";
      LocalTime start = LocalTime.of(8, 0);
      LocalTime end = LocalTime.of(17, 0);

      Employee employee = Employee.working(name, EmploymentType.MANAGER, start, end);

      assertEquals(name, employee.name());
      assertEquals(start, employee.start());
      assertEquals(end, employee.end());
      assertEquals(false, employee.off());
      assertEquals(EmploymentType.MANAGER, employee.employmentType());
    }

    @Test
    @DisplayName(
        "[F-1] Given: Employee.onLeave(name, type)で従業員を作成するとき, When: 属性にアクセスすると,"
            + " Then: employmentTypeに指定した型が保持される")
    void onLeaveEmployeeWithSpecifiedType() {
      String name = "太郎";

      Employee employee = Employee.onLeave(name, EmploymentType.PART_TIME);

      assertEquals(name, employee.name());
      assertEquals(true, employee.off());
      assertNull(employee.start());
      assertNull(employee.end());
      assertEquals(EmploymentType.PART_TIME, employee.employmentType());
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
      Employee employee =
          new Employee("太郎", EmploymentType.FULL_TIME, false, null, LocalTime.of(17, 0));

      assertEquals(false, employee.canWork(ShiftSlot.SLOT_1));
    }

    @Test
    @DisplayName("[H-3] Given: endがnullの従業員のとき, When: canWorkを呼ぶと, Then: falseである")
    void canWorkReturnsFalseWhenEndIsNull() {
      Employee employee =
          new Employee("太郎", EmploymentType.FULL_TIME, false, LocalTime.of(8, 0), null);

      assertEquals(false, employee.canWork(ShiftSlot.SLOT_1));
    }
  }

  @Nested
  @DisplayName("[H-3] 従業員が入れる枠を列挙")
  class WorkableSlots {

    @Test
    @DisplayName("[H-3] Given: 7:30〜18:30の従業員のとき, When: workableSlotsを呼ぶと, Then:" + " 枠1〜6をその順で返す")
    void workableSlotsReturnsAllSlotsInOrder() {
      Employee employee = Employee.working("太郎", LocalTime.of(7, 30), LocalTime.of(18, 30));

      var slots = employee.workableSlots();

      assertEquals(6, slots.size());
      assertEquals(ShiftSlot.SLOT_1, slots.get(0));
      assertEquals(ShiftSlot.SLOT_2, slots.get(1));
      assertEquals(ShiftSlot.SLOT_3, slots.get(2));
      assertEquals(ShiftSlot.SLOT_4, slots.get(3));
      assertEquals(ShiftSlot.SLOT_5, slots.get(4));
      assertEquals(ShiftSlot.SLOT_6, slots.get(5));
    }

    @Test
    @DisplayName("[H-3] Given: 9:00〜16:30の従業員のとき, When: workableSlotsを呼ぶと, Then:" + " 枠4だけを返す")
    void workableSlotsReturnsOnlyMatchingSlots() {
      Employee employee = Employee.working("太郎", LocalTime.of(9, 0), LocalTime.of(16, 30));

      var slots = employee.workableSlots();

      assertEquals(1, slots.size());
      assertEquals(ShiftSlot.SLOT_4, slots.get(0));
    }

    @Test
    @DisplayName("[H-3] Given: 休みの従業員のとき, When: workableSlotsを呼ぶと, Then:" + " 空のリストを返す")
    void workableSlotsReturnsEmptyForOnLeaveEmployee() {
      Employee employee = Employee.onLeave("太郎");

      var slots = employee.workableSlots();

      assertEquals(0, slots.size());
    }

    @Test
    @DisplayName("[H-3] Given: 開始・終了がnullの従業員のとき, When: workableSlotsを呼ぶと, Then:" + " 空のリストを返す")
    void workableSlotsReturnsEmptyWhenTimeRangeIsNull() {
      Employee employee = new Employee("太郎", EmploymentType.FULL_TIME, false, null, null);

      var slots = employee.workableSlots();

      assertEquals(0, slots.size());
    }

    @Test
    @DisplayName(
        "[H-3] Given: 8:00〜15:30の従業員（枠2と境界が一致）のとき, When: workableSlotsを呼ぶと, Then:" + " 枠2を含む")
    void workableSlotsIncludeSlotWhenBoundariesMatch() {
      Employee employee = Employee.working("太郎", LocalTime.of(8, 0), LocalTime.of(15, 30));

      var slots = employee.workableSlots();

      assertEquals(1, slots.size());
      assertEquals(ShiftSlot.SLOT_2, slots.get(0));
    }

    @Test
    @DisplayName(
        "[H-3] Given: 同じ時間帯の従業員が常勤・パート・管理職のときときき, When: canWorkの結果を比較すると,"
            + " Then: 雇用区分に関わらず結果が同じである")
    void canWorkDoesNotDependOnEmploymentType() {
      LocalTime start = LocalTime.of(8, 0);
      LocalTime end = LocalTime.of(17, 0);

      Employee fullTime = Employee.working("太郎", start, end);
      Employee partTime = Employee.working("花子", EmploymentType.PART_TIME, start, end);
      Employee manager = Employee.working("次郎", EmploymentType.MANAGER, start, end);

      for (ShiftSlot slot : ShiftSlot.values()) {
        assertEquals(fullTime.canWork(slot), partTime.canWork(slot), "常勤とパートで canWork の結果が異なります");
        assertEquals(fullTime.canWork(slot), manager.canWork(slot), "常勤と管理職で canWork の結果が異なります");
      }
    }
  }

  @Nested
  @DisplayName("[F-4][H-3] 未出勤の理由を判定")
  class UnassignedReasonTest {

    @Test
    @DisplayName("[F-4] Given: 休みの従業員K, When: unassignedReason()を呼ぶと, Then: ON_LEAVE を返す")
    void onLeaveEmployeeReturnsOnLeave() {
      Employee employee = Employee.onLeave("K");

      assertEquals(UnassignedReason.ON_LEAVE, employee.unassignedReason());
    }

    @Test
    @DisplayName(
        "[F-4][H-3] Given: 開始・終了がnullの従業員, When: unassignedReason()を呼ぶと, Then:"
            + " NO_AVAILABLE_SLOT を返す")
    void nullTimeRangeReturnsNoAvailableSlot() {
      Employee employee = new Employee("X", EmploymentType.FULL_TIME, false, null, null);

      assertEquals(UnassignedReason.NO_AVAILABLE_SLOT, employee.unassignedReason());
    }

    @Test
    @DisplayName(
        "[F-4][H-3] Given: 9:00〜10:00の従業員（どの枠にも入らない), When: unassignedReason()を呼ぶと, Then:"
            + " NO_AVAILABLE_SLOT を返す")
    void noSuitableSlotReturnsNoAvailableSlot() {
      Employee employee =
          Employee.working("J", java.time.LocalTime.of(9, 0), java.time.LocalTime.of(10, 0));

      assertEquals(UnassignedReason.NO_AVAILABLE_SLOT, employee.unassignedReason());
    }

    @Test
    @DisplayName(
        "[F-4][H-3] Given: 7:30〜18:30の従業員（入れる枠がある), When: unassignedReason()を呼ぶと, Then:"
            + " LOWER_GAP_CHOSEN を返す")
    void withWorkableSlotsReturnsLowerGapChosen() {
      Employee employee =
          Employee.working("I", java.time.LocalTime.of(7, 30), java.time.LocalTime.of(18, 30));

      assertEquals(UnassignedReason.LOWER_GAP_CHOSEN, employee.unassignedReason());
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

    @Test
    @DisplayName(
        "[H-3] Given: 同じ時間帯の従業員が常勤・パート・管理職のときときき, When: gapMinutesの結果を比較すると,"
            + " Then: 雇用区分に関わらず結果が同じである")
    void gapMinutesDoesNotDependOnEmploymentType() {
      LocalTime start = LocalTime.of(8, 0);
      LocalTime end = LocalTime.of(17, 0);

      Employee fullTime = Employee.working("太郎", start, end);
      Employee partTime = Employee.working("花子", EmploymentType.PART_TIME, start, end);
      Employee manager = Employee.working("次郎", EmploymentType.MANAGER, start, end);

      assertEquals(90, fullTime.gapMinutes(ShiftSlot.SLOT_2));
      assertEquals(90, partTime.gapMinutes(ShiftSlot.SLOT_2));
      assertEquals(90, manager.gapMinutes(ShiftSlot.SLOT_2));
    }
  }

  @Nested
  @DisplayName("[H-4] 週の残り時間による割り当て可否の判定")
  class CanAssign {

    @Test
    @DisplayName(
        "[H-4] Given: 週の残り時間がnull（上限なし）の従業員のとき, When: canAssignを呼ぶと, Then:" + " canWorkと同じ結果である")
    void canAssignMatchesCanWorkWhenRemainingMinutesIsNull() {
      Employee employee = Employee.working("太郎", LocalTime.of(7, 30), LocalTime.of(18, 30));

      for (ShiftSlot slot : ShiftSlot.values()) {
        assertEquals(employee.canWork(slot), employee.canAssign(slot));
      }
    }

    @Test
    @DisplayName("[H-4] Given: パートで週の残り時間が405分の従業員のとき, When: 枠2（405分）でcanAssignを呼ぶと, Then: trueである")
    void canAssignReturnsTrueWhenRemainingMinutesEqualsSlotActualWorkMinutes() {
      Employee employee =
          Employee.working(
                  "太郎", EmploymentType.PART_TIME, LocalTime.of(7, 30), LocalTime.of(18, 30))
              .withWeeklyRemainingMinutes(405);

      assertEquals(true, employee.canAssign(ShiftSlot.SLOT_2));
    }

    @Test
    @DisplayName(
        "[H-4] Given: パートで週の残り時間が405分の従業員のとき, When: 枠3（435分）でcanAssignを呼ぶと, Then: falseである")
    void canAssignReturnsFalseWhenRemainingMinutesIsLessThanSlotActualWorkMinutes() {
      Employee employee =
          Employee.working(
                  "太郎", EmploymentType.PART_TIME, LocalTime.of(7, 30), LocalTime.of(18, 30))
              .withWeeklyRemainingMinutes(405);

      assertEquals(false, employee.canAssign(ShiftSlot.SLOT_3));
    }

    @Test
    @DisplayName(
        "[H-3][H-4] Given: H-3を満たさない枠で、週の残り時間が十分なパートのとき, When: canAssignを呼ぶと, Then:" + " falseである")
    void canAssignReturnsFalseWhenCanWorkIsFalseEvenWithEnoughRemainingMinutes() {
      Employee employee =
          Employee.working("太郎", EmploymentType.PART_TIME, LocalTime.of(8, 0), LocalTime.of(17, 0))
              .withWeeklyRemainingMinutes(1200);

      assertEquals(false, employee.canAssign(ShiftSlot.SLOT_1));
    }
  }

  @Nested
  @DisplayName("[H-4] 週の残り時間の判定はパートだけに適用する")
  class CanAssignAppliesOnlyToPartTime {

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(
        value = EmploymentType.class,
        names = {"FULL_TIME", "MANAGER"})
    @DisplayName(
        "[H-4] Given: 常勤・管理職に週の残り時間300分（枠1の実労働時間375分未満）を設定したとき, When:"
            + " H-3を満たす枠1でcanAssignを呼ぶと, Then: trueであり、unassignedReasonがWEEKLY_LIMIT_EXCEEDEDにならない")
    void doesNotLimitFullTimeOrManager(EmploymentType employmentType) {
      Employee employee =
          Employee.working("太郎", employmentType, LocalTime.of(7, 30), LocalTime.of(14, 30))
              .withWeeklyRemainingMinutes(300);

      assertEquals(true, employee.canAssign(ShiftSlot.SLOT_1));
      assertEquals(UnassignedReason.LOWER_GAP_CHOSEN, employee.unassignedReason());
    }
  }

  @Nested
  @DisplayName("[5.6] これまでの出勤日数の保持")
  class PriorWorkDays {

    @Test
    @DisplayName(
        "[5.6] Given: 常勤の従業員, When: withPriorWorkDaysを呼ぶと, Then:" + " priorWorkDaysが設定され他の値は変わらない")
    void withPriorWorkDaysSetsDaysAndKeepsOtherValues() {
      Employee employee =
          Employee.working(
              "太郎", EmploymentType.PART_TIME, LocalTime.of(7, 30), LocalTime.of(18, 30));

      Employee updated = employee.withPriorWorkDays(3);

      assertEquals(3, updated.priorWorkDays());
      assertEquals(employee.name(), updated.name());
      assertEquals(employee.employmentType(), updated.employmentType());
      assertEquals(employee.off(), updated.off());
      assertEquals(employee.start(), updated.start());
      assertEquals(employee.end(), updated.end());
      assertEquals(employee.weeklyRemainingMinutes(), updated.weeklyRemainingMinutes());
    }

    @Test
    @DisplayName(
        "[5.6] Given: withPriorWorkDaysで出勤日数を設定した従業員, When: withWeeklyRemainingMinutesを呼ぶと,"
            + " Then: 週の残り時間だけが変わり、priorWorkDaysは保たれる")
    void withWeeklyRemainingMinutesKeepsPriorWorkDays() {
      Employee employee =
          Employee.working(
                  "花子", EmploymentType.PART_TIME, LocalTime.of(7, 30), LocalTime.of(18, 30))
              .withPriorWorkDays(5);

      Employee updated = employee.withWeeklyRemainingMinutes(600);

      assertEquals(5, updated.priorWorkDays());
      assertEquals(600, updated.weeklyRemainingMinutes());
    }

    @Test
    @DisplayName(
        "[5.6] Given: withPriorWorkDaysで出勤日数を設定した従業員, When: withPriorWorkDaysを再度呼ぶと,"
            + " Then: 週の残り時間は保たれる")
    void withPriorWorkDaysKeepsWeeklyRemainingMinutes() {
      Employee employee =
          Employee.working(
                  "次郎", EmploymentType.PART_TIME, LocalTime.of(7, 30), LocalTime.of(18, 30))
              .withWeeklyRemainingMinutes(600);

      Employee updated = employee.withPriorWorkDays(2);

      assertEquals(2, updated.priorWorkDays());
      assertEquals(600, updated.weeklyRemainingMinutes());
    }

    @Test
    @DisplayName("[5.6] Given: 既存のコンストラクタ・ファクトリで作成した従業員, When: priorWorkDaysを確認すると, Then: nullである")
    void existingConstructorsLeavePriorWorkDaysNull() {
      Employee viaWorking = Employee.working("A", LocalTime.of(7, 30), LocalTime.of(18, 30));
      assertNull(viaWorking.priorWorkDays());

      Employee viaWorkingWithType =
          Employee.working(
              "B", EmploymentType.PART_TIME, LocalTime.of(7, 30), LocalTime.of(18, 30));
      assertNull(viaWorkingWithType.priorWorkDays());

      Employee viaOnLeave = Employee.onLeave("C");
      assertNull(viaOnLeave.priorWorkDays());

      Employee viaOnLeaveWithType = Employee.onLeave("D", EmploymentType.PART_TIME);
      assertNull(viaOnLeaveWithType.priorWorkDays());

      Employee viaFiveArgConstructor =
          new Employee(
              "E", EmploymentType.FULL_TIME, false, LocalTime.of(7, 30), LocalTime.of(18, 30));
      assertNull(viaFiveArgConstructor.priorWorkDays());

      Employee viaFourArgConstructor =
          new Employee("F", false, LocalTime.of(7, 30), LocalTime.of(18, 30));
      assertNull(viaFourArgConstructor.priorWorkDays());
    }
  }

  @Nested
  @DisplayName("[7.2][H-4] 未出勤の理由の判定順（休み → 入れる枠なし → 週上限超え → その他）")
  class UnassignedReasonOrder {

    @Test
    @DisplayName("[7.2][H-4] Given: 休みの従業員, When: unassignedReason()を呼ぶと, Then: ON_LEAVE を返す")
    void offReturnsOnLeave() {
      Employee employee = Employee.onLeave("N", EmploymentType.PART_TIME);

      assertEquals(UnassignedReason.ON_LEAVE, employee.unassignedReason());
    }

    @Test
    @DisplayName(
        "[7.2][H-4] Given: H-3を満たす枠がない従業員, When: unassignedReason()を呼ぶと, Then:"
            + " NO_AVAILABLE_SLOT を返す")
    void noWorkableSlotReturnsNoAvailableSlot() {
      Employee employee =
          Employee.working("O", EmploymentType.PART_TIME, LocalTime.of(9, 0), LocalTime.of(10, 0));

      assertEquals(UnassignedReason.NO_AVAILABLE_SLOT, employee.unassignedReason());
    }

    @Test
    @DisplayName(
        "[7.2][H-4] Given: H-3を満たす枠はあるが週の残り時間が足りないパート, When: unassignedReason()を呼ぶと, Then:"
            + " WEEKLY_LIMIT_EXCEEDED を返す")
    void workableSlotButOverWeeklyLimitReturnsWeeklyLimitExceeded() {
      Employee employee =
          Employee.working("P", EmploymentType.PART_TIME, LocalTime.of(7, 30), LocalTime.of(14, 30))
              .withWeeklyRemainingMinutes(300);

      assertEquals(UnassignedReason.WEEKLY_LIMIT_EXCEEDED, employee.unassignedReason());
    }

    @Test
    @DisplayName(
        "[7.2][H-4] Given: 入れる枠があり週の残り時間も十分な従業員, When: unassignedReason()を呼ぶと, Then:"
            + " LOWER_GAP_CHOSEN を返す")
    void workableSlotWithinWeeklyLimitReturnsLowerGapChosen() {
      Employee employee =
          Employee.working("Q", EmploymentType.PART_TIME, LocalTime.of(7, 30), LocalTime.of(14, 30))
              .withWeeklyRemainingMinutes(375);

      assertEquals(UnassignedReason.LOWER_GAP_CHOSEN, employee.unassignedReason());
    }
  }
}
