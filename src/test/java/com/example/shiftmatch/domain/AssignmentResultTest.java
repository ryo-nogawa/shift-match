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
    @DisplayName("Given: 8件の割り当てが与えられたとき, When: AssignmentResultを生成すると, Then: 割り当てがそれぞれ取得できる")
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
        "Given: 割り当ての件数が8でないとき, When: AssignmentResultを生成すると, Then:"
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
    @DisplayName("Given: 割り当てが与えられたとき, When: assignmentsにアクセスすると, Then: 不変なリストが返される")
    void returnsImmutableAssignments() {
      List<ShiftAssignment> assignments = createStandardAssignments();
      int score = 0;
      List<Employee> unassigned = new ArrayList<>();

      AssignmentResult result = new AssignmentResult(assignments, score, unassigned);
      List<ShiftAssignment> returnedAssignments = result.assignments();

      // Try to modify should throw
      assertThrows(
          UnsupportedOperationException.class,
          () -> returnedAssignments.add(createAssignment(8, ShiftSlot.SLOT_6)));
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
        new Employee(
            "Employee" + id,
            List.of(
                Wish.AVAILABLE,
                Wish.AVAILABLE,
                Wish.AVAILABLE,
                Wish.AVAILABLE,
                Wish.AVAILABLE,
                Wish.AVAILABLE));
    return new ShiftAssignment(employee, slot, LocalTime.of(12, 0), LocalTime.of(12, 45));
  }
}
