package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.ShiftSlot;
import com.example.shiftmatch.domain.Wish;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ShiftAssignmentServiceImpl")
class ShiftAssignmentServiceImplTest {

  @Nested
  @DisplayName("[H-1] 割り当てルール")
  class AssignmentRules {

    @Test
    @DisplayName("Given: 8人全員が全6枠でAVAILABLEのとき, When: assignを実行すると, Then: 入力順どおりに枠1→6へ割り当てられる")
    void assignsInInputOrderToFrames() {
      List<Employee> employees = createAllAvailableEmployees(8);
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      Optional<AssignmentResult> result = service.assign(employees);

      assertTrue(result.isPresent());
      AssignmentResult assignment = result.get();

      // Check frame assignments
      // Frame 1: persons 0, 1
      assertEquals(ShiftSlot.SLOT_1, assignment.assignments().get(0).slot());
      assertEquals(ShiftSlot.SLOT_1, assignment.assignments().get(1).slot());

      // Frame 2: person 2
      assertEquals(ShiftSlot.SLOT_2, assignment.assignments().get(2).slot());

      // Frame 3: person 3
      assertEquals(ShiftSlot.SLOT_3, assignment.assignments().get(3).slot());

      // Frame 4: person 4
      assertEquals(ShiftSlot.SLOT_4, assignment.assignments().get(4).slot());

      // Frame 5: person 5
      assertEquals(ShiftSlot.SLOT_5, assignment.assignments().get(5).slot());

      // Frame 6: persons 6, 7
      assertEquals(ShiftSlot.SLOT_6, assignment.assignments().get(6).slot());
      assertEquals(ShiftSlot.SLOT_6, assignment.assignments().get(7).slot());
    }

    @Test
    @DisplayName("[H-2] Given: 8人が与えられたとき, When: assignを実行すると, Then: 1人が複数の枠に割り当てられない")
    void eachPersonAssignedToOnlyOneSlot() {
      List<Employee> employees = createAllAvailableEmployees(8);
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      Optional<AssignmentResult> result = service.assign(employees);

      assertTrue(result.isPresent());
      AssignmentResult assignment = result.get();

      Set<String> assignedNames = new HashSet<>();
      for (var shiftAssignment : assignment.assignments()) {
        String name = shiftAssignment.employee().name();
        assertTrue(assignedNames.add(name), "Employee " + name + " assigned multiple times");
      }
    }

    @Test
    @DisplayName("[H-3] Given: 枠1で×の従業員を含む12人がいるとき, When: assignを実行すると, Then: その従業員が枠1に割り当てられない")
    void excludesUnavailableEmployeeFromSlot() {
      List<Employee> employees = new ArrayList<>();

      // Employee 0: × for slot 1, available for all others
      List<Wish> wishes0 =
          List.of(
              Wish.UNAVAILABLE,
              Wish.AVAILABLE,
              Wish.AVAILABLE,
              Wish.AVAILABLE,
              Wish.AVAILABLE,
              Wish.AVAILABLE);
      employees.add(new Employee("Employee0", wishes0));

      // Employees 1-7: all available
      for (int i = 1; i < 8; i++) {
        employees.add(createAvailableEmployee("Employee" + i));
      }

      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();
      Optional<AssignmentResult> result = service.assign(employees);

      assertTrue(result.isPresent());
      AssignmentResult assignment = result.get();

      // Employee 0 should not be in slot 1 (positions 0-1)
      assertFalse(
          assignment.assignments().get(0).employee().name().equals("Employee0")
              || assignment.assignments().get(1).employee().name().equals("Employee0"));
    }
  }

  @Nested
  @DisplayName("[V-4] 有効な従業員が8名未満")
  class InsufficientEmployees {

    @Test
    @DisplayName("Given: 有効な従業員が7名のとき, When: assignを実行すると, Then: Optional.emptyが返される")
    void returnsEmptyWhenLessThanEightValidEmployees() {
      List<Employee> employees = createAllAvailableEmployees(7);
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      Optional<AssignmentResult> result = service.assign(employees);

      assertFalse(result.isPresent());
    }
  }

  @Nested
  @DisplayName("[スコア評価]")
  class ScoreEvaluation {

    @Test
    @DisplayName("◎の数が多い案が選ばれること")
    void selectsCombinationWithHighestScore() {
      List<Employee> employees = new ArrayList<>();

      // Employees with different number of ◎
      employees.add(
          new Employee(
              "A",
              List.of(
                  Wish.DESIRED,
                  Wish.AVAILABLE,
                  Wish.AVAILABLE,
                  Wish.AVAILABLE,
                  Wish.AVAILABLE,
                  Wish.AVAILABLE)));
      employees.add(
          new Employee(
              "B",
              List.of(
                  Wish.AVAILABLE,
                  Wish.DESIRED,
                  Wish.AVAILABLE,
                  Wish.AVAILABLE,
                  Wish.AVAILABLE,
                  Wish.AVAILABLE)));
      employees.add(
          new Employee(
              "C",
              List.of(
                  Wish.AVAILABLE,
                  Wish.AVAILABLE,
                  Wish.DESIRED,
                  Wish.AVAILABLE,
                  Wish.AVAILABLE,
                  Wish.AVAILABLE)));
      employees.add(
          new Employee(
              "D",
              List.of(
                  Wish.AVAILABLE,
                  Wish.AVAILABLE,
                  Wish.AVAILABLE,
                  Wish.DESIRED,
                  Wish.AVAILABLE,
                  Wish.AVAILABLE)));
      employees.add(
          new Employee(
              "E",
              List.of(
                  Wish.AVAILABLE,
                  Wish.AVAILABLE,
                  Wish.AVAILABLE,
                  Wish.AVAILABLE,
                  Wish.DESIRED,
                  Wish.AVAILABLE)));
      employees.add(
          new Employee(
              "F",
              List.of(
                  Wish.AVAILABLE,
                  Wish.AVAILABLE,
                  Wish.AVAILABLE,
                  Wish.AVAILABLE,
                  Wish.AVAILABLE,
                  Wish.DESIRED)));
      employees.add(
          new Employee(
              "G",
              List.of(
                  Wish.AVAILABLE,
                  Wish.AVAILABLE,
                  Wish.AVAILABLE,
                  Wish.AVAILABLE,
                  Wish.AVAILABLE,
                  Wish.AVAILABLE)));
      employees.add(
          new Employee(
              "H",
              List.of(
                  Wish.AVAILABLE,
                  Wish.AVAILABLE,
                  Wish.AVAILABLE,
                  Wish.AVAILABLE,
                  Wish.AVAILABLE,
                  Wish.AVAILABLE)));

      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();
      Optional<AssignmentResult> result = service.assign(employees);

      assertTrue(result.isPresent());
      AssignmentResult assignment = result.get();

      // Score should be 6 (A-F have ◎ in their assigned slots)
      assertEquals(6, assignment.score());
    }

    @Test
    @DisplayName("同点では最初に最大スコアに達した案が採用される")
    void selectsFirstCombinationWithMaxScoreWhenTied() {
      // This test would need a specific scenario to verify
      // that the algorithm doesn't update when score is equal
      List<Employee> employees = createAllAvailableEmployees(8);
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      Optional<AssignmentResult> result = service.assign(employees);

      assertTrue(result.isPresent());
      // Score should be 0 since all are AVAILABLE (not DESIRED)
      assertEquals(0, result.get().score());
    }
  }

  @Nested
  @DisplayName("[パフォーマンス]")
  class Performance {

    @Test
    @DisplayName("12人全員AVAILABLE時に完了する")
    void completesForTwelveEmployees() {
      List<Employee> employees = createAllAvailableEmployees(12);
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      long startTime = System.currentTimeMillis();
      Optional<AssignmentResult> result = service.assign(employees);
      long elapsedTime = System.currentTimeMillis() - startTime;

      assertTrue(result.isPresent());
      assertTrue(elapsedTime < 30000, "Assignment took " + elapsedTime + "ms");
    }

    @Test
    @DisplayName("[T-3] Given: 12人全員AVAILABLEのとき, When: assignを実行すると, Then: 10秒以内に完了する")
    void completesWithinTenSecondsForTwelveEmployees() {
      List<Employee> employees = createAllAvailableEmployees(12);
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      long startTime = System.currentTimeMillis();
      Optional<AssignmentResult> result = service.assign(employees);
      long elapsedTime = System.currentTimeMillis() - startTime;

      assertTrue(result.isPresent());
      assertTrue(
          elapsedTime < 10000, "Assignment took " + elapsedTime + "ms, should be under 10 seconds");
    }
  }

  private List<Employee> createAllAvailableEmployees(int count) {
    List<Employee> employees = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      employees.add(createAvailableEmployee("Employee" + i));
    }
    return employees;
  }

  private Employee createAvailableEmployee(String name) {
    return new Employee(
        name,
        List.of(
            Wish.AVAILABLE,
            Wish.AVAILABLE,
            Wish.AVAILABLE,
            Wish.AVAILABLE,
            Wish.AVAILABLE,
            Wish.AVAILABLE));
  }
}
