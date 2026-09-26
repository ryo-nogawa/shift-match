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
        "[F-4] Given: 各割り当てのずれが計算されるとき, When: gapMinutesListを呼ぶと, Then:" + " 合計がscoreに一致する")
    void gapMinutesListSumEqualsScore() {
      List<ShiftAssignment> assignments = createStandardAssignments();
      // すべての従業員が 7:30-18:30（660分）で、各枠に割り当てられた場合のずれ：
      // createStandardAssignments() の各割り当ての計算結果が、
      // gapMinutesList() の合計と一致することを確認する
      // 実装と計算結果が一致することが条件
      int expectedScore = 1230;
      AssignmentResult result = new AssignmentResult(assignments, expectedScore, new ArrayList<>());

      var gapList = result.gapMinutesList();
      int sumOfGaps = gapList.stream().mapToInt(i -> i.intValue()).sum();

      assertEquals(expectedScore, sumOfGaps);
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
