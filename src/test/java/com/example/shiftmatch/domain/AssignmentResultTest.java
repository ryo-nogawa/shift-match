package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AssignmentResult")
class AssignmentResultTest {

  @Nested
  @DisplayName("[H-1] 割り当て結果")
  class AssignmentConstruction {

    @Test
    @DisplayName("[H-1] Given: 8件の割り当てが与えられたとき, When: AssignmentResultを生成すると, Then: 割り当てがそれぞれ取得できる")
    void canCreateAssignmentResultWithEightAssignments() {
      List<ShiftAssignment> assignments = createStandardAssignments();
      int score = 5;
      List<Employee> unassigned = new ArrayList<>();

      AssignmentResult result = new AssignmentResult(assignments, score, unassigned);

      assertEquals(8, result.assignments().size());
      assertEquals(score, result.score());
      assertEquals(0, result.unassignedEmployees().size());
    }

    @Test
    @DisplayName(
        "[H-1] Given: 割り当ての件数が8でないとき, When: AssignmentResultを生成すると, Then:"
            + " IllegalArgumentExceptionがスローされる")
    void throwsExceptionWhenAssignmentsCountIsNotEight() {
      List<ShiftAssignment> assignments = new ArrayList<>();
      assignments.add(createAssignment(0, ShiftSlot.SLOT_1));
      assignments.add(createAssignment(1, ShiftSlot.SLOT_1));

      int score = 0;
      List<Employee> unassigned = new ArrayList<>();

      assertThrows(
          IllegalArgumentException.class,
          () -> new AssignmentResult(assignments, score, unassigned));
    }

    @Test
    @DisplayName("[H-1] Given: 割り当てが与えられたとき, When: assignmentsにアクセスすると, Then: 不変なリストが返される")
    void returnsImmutableAssignments() {
      List<ShiftAssignment> assignments = createStandardAssignments();
      int score = 0;
      List<Employee> unassigned = new ArrayList<>();

      AssignmentResult result = new AssignmentResult(assignments, score, unassigned);
      List<ShiftAssignment> returnedAssignments = result.assignments();

      assertThrows(
          UnsupportedOperationException.class,
          () -> returnedAssignments.add(createAssignment(8, ShiftSlot.SLOT_6)));
    }
  }

  @Nested
  @DisplayName("[F-4] ずれの合計をリストで返す")
  class GapMinutesList {

    @Test
    @DisplayName("[F-4] Given: 8件の割り当てのとき, When: gapMinutesListを呼ぶと, Then: 8件のずれを返す")
    void gapMinutesListReturnsSameCount() {
      List<ShiftAssignment> assignments = createStandardAssignments();
      int score = 0;
      List<Employee> unassigned = new ArrayList<>();
      AssignmentResult result = new AssignmentResult(assignments, score, unassigned);

      var gapList = result.gapMinutesList();

      assertEquals(8, gapList.size());
    }

    @Test
    @DisplayName(
        "[F-4] Given: 各割り当てのずれが計算されるとき, When: gapMinutesListを呼ぶと, Then:" + " 期待される各人のずれが返される")
    void gapMinutesListReturnsCorrectGaps() {
      List<ShiftAssignment> assignments = createStandardAssignments();
      // すべての従業員が 7:30-18:30（660分）に設定されている
      // 各枠の勤務時間から手計算したずれ：
      // i=0：枠1(420分) → gap = 660-420 = 240
      // i=1：枠2(450分) → gap = 660-450 = 210
      // i=2：枠3(480分) → gap = 660-480 = 180
      // i=3：枠4(450分) → gap = 660-450 = 210
      // i=4：枠5(540分) → gap = 660-540 = 120
      // i=5：枠6(570分) → gap = 660-570 = 90
      // i=6：枠6(570分) → gap = 660-570 = 90  （Math.min(6, 5) = 5 → 枠6）
      // i=7：枠6(570分) → gap = 660-570 = 90  （Math.min(7, 5) = 5 → 枠6）
      List<Integer> expectedGaps = List.of(240, 210, 180, 210, 120, 90, 90, 90);
      int expectedScore = 1230;
      AssignmentResult result = new AssignmentResult(assignments, expectedScore, new ArrayList<>());

      List<Integer> actualGaps = result.gapMinutesList();

      assertEquals(expectedGaps, actualGaps, "ずれのリストが期待値と一致する");
      int sumOfGaps = actualGaps.stream().mapToInt(i -> i.intValue()).sum();
      assertEquals(expectedScore, sumOfGaps, "ずれの合計がスコアと一致する");
    }
  }

  @Nested
  @DisplayName("[F-4][F-3] 未出勤者の理由")
  class UnassignedReasonLabel {

    private AssignmentResult resultWithUnassigned(Employee unassigned) {
      return new AssignmentResult(createStandardAssignments(), 0, List.of(unassigned));
    }

    @Test
    @DisplayName(
        "[F-3][F-4] Given: 未出勤者が割り当て済みの人と入れ替えてもずれの合計が変わらないとき,"
            + " When: 理由を取得すると, Then: 同じずれの案があり、優先度が高い割り当て済みの人が選ばれたと氏名つきで示す")
    void namesTheAssignedEmployeeWhenSwapKeepsTotalGap() {
      // Employee0 は枠 1 で 660 分の時間帯。同じ 660 分の Ito も枠 1 に入れ、ずれは 240 分で同じになる
      Employee ito = Employee.working("Ito", LocalTime.of(7, 30), LocalTime.of(18, 30));

      String label = resultWithUnassigned(ito).unassignedReasonLabel(ito);

      assertEquals("入れる枠はあったが、同じずれの案があり、入力順で優先度が高い Employee0 が選ばれた", label);
    }

    @Test
    @DisplayName(
        "[F-4] Given: どの割り当て済みの人と入れ替えてもずれの合計が変わるとき," + " When: 理由を取得すると, Then: より小さいずれの案が選ばれたと示す")
    void saysLowerGapWhenNoSwapKeepsTotalGap() {
      // 7:30〜18:00（630 分）は枠 1〜5 に入れるが、どの枠でも 660 分の人とはずれが異なる
      Employee shorter = Employee.working("Short", LocalTime.of(7, 30), LocalTime.of(18, 0));

      String label = resultWithUnassigned(shorter).unassignedReasonLabel(shorter);

      assertEquals("入れる枠はあったが、より小さいずれの案が選ばれた", label);
    }

    @Test
    @DisplayName("[F-4] Given: 未出勤者が休みのとき, When: 理由を取得すると, Then: 「休み」を返す")
    void returnsOffLabelForEmployeeOnLeave() {
      Employee off = Employee.onLeave("Off");

      assertEquals("休み", resultWithUnassigned(off).unassignedReasonLabel(off));
    }

    @Test
    @DisplayName("[F-4] Given: 未出勤者がどの枠にも入れないとき, When: 理由を取得すると, Then: 「どの枠にも入れない」を返す")
    void returnsNoAvailableSlotLabel() {
      Employee narrow = Employee.working("Narrow", LocalTime.of(9, 0), LocalTime.of(10, 0));

      assertEquals("どの枠にも入れない", resultWithUnassigned(narrow).unassignedReasonLabel(narrow));
    }
  }

  private List<ShiftAssignment> createStandardAssignments() {
    List<ShiftAssignment> assignments = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      assignments.add(createAssignment(i, ShiftSlot.values()[Math.min(i, 5)]));
    }
    return assignments;
  }

  private ShiftAssignment createAssignment(int id, ShiftSlot slot) {
    Employee employee =
        Employee.working("Employee" + id, LocalTime.of(7, 30), LocalTime.of(18, 30));
    return new ShiftAssignment(employee, slot, LocalTime.of(12, 0), LocalTime.of(12, 45));
  }
}
