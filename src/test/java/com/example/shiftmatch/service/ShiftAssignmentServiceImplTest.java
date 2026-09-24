package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.ShiftSlot;
import com.example.shiftmatch.domain.Wish;
import java.time.Duration;
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
    @DisplayName("[H-3] Given: 枠1で×の従業員を含むとき, When: assignを実行すると, Then: その従業員が枠1に割り当てられない")
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

    @Test
    @DisplayName("[H-3] Given: 8名中1名が全枠×、他の7名は全枠◯のとき, When: assignを実行すると, Then: Optional.emptyになる")
    void returnsEmptyWhenOneEmployeeAllUnavailableAndOthersCannotFillAllSlots() {
      List<Employee> employees = new ArrayList<>();

      // Employee 0: × for all slots
      List<Wish> wishesAllUnavailable =
          List.of(
              Wish.UNAVAILABLE,
              Wish.UNAVAILABLE,
              Wish.UNAVAILABLE,
              Wish.UNAVAILABLE,
              Wish.UNAVAILABLE,
              Wish.UNAVAILABLE);
      employees.add(new Employee("Employee0", wishesAllUnavailable));

      // Employees 1-7: all available
      for (int i = 1; i < 8; i++) {
        employees.add(createAvailableEmployee("Employee" + i));
      }

      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();
      Optional<AssignmentResult> result = service.assign(employees);

      // Should return empty because only 7 valid employees remain
      assertFalse(result.isPresent(), "Should return empty when only 7 valid employees remain");
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
    @DisplayName(
        "[5.2] Given: ある従業員だけが枠1に◎を付けたとき, When: assignを実行すると, Then: その従業員が枠1に割り当てられ、scoreが1になる")
    void assignsEmployeeWithDesiredSlotAndScoringOne() {
      List<Employee> employees = new ArrayList<>();
      // Employee A: Desired for slot 1
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
      // Employees B-H: all available for all slots
      for (int i = 1; i < 8; i++) {
        employees.add(createAvailableEmployee((char) ('A' + i) + ""));
      }

      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();
      Optional<AssignmentResult> result = service.assign(employees);

      assertTrue(result.isPresent());
      AssignmentResult assignment = result.get();

      // Employee A should be in slot 1 (position 0 or 1)
      boolean foundEmployeeInSlot1 = false;
      for (int i = 0; i < 2; i++) {
        if (assignment.assignments().get(i).employee().name().equals("A")
            && assignment.assignments().get(i).slot() == ShiftSlot.SLOT_1) {
          foundEmployeeInSlot1 = true;
          break;
        }
      }
      assertTrue(foundEmployeeInSlot1, "Employee A should be assigned to slot 1");
      assertEquals(1, assignment.score(), "Score should be 1 since only A has ◎");
    }

    @Test
    @DisplayName(
        "[5.3] Given: 全員が全枠◎を付けずに◯で、スコア0のとき, When: assignを実行すると, Then: 入力順どおりに枠1→6へ割り当てられる")
    void selectsFirstAssignmentWhenAllTiedAtScoreZero() {
      List<Employee> employees = createAllAvailableEmployees(8);
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      Optional<AssignmentResult> result = service.assign(employees);

      assertTrue(result.isPresent());
      AssignmentResult assignment = result.get();

      // Verify input order assignment (Employee0, Employee1, etc.)
      assertEquals("Employee0", assignment.assignments().get(0).employee().name());
      assertEquals("Employee1", assignment.assignments().get(1).employee().name());
      assertEquals("Employee2", assignment.assignments().get(2).employee().name());
      assertEquals("Employee3", assignment.assignments().get(3).employee().name());
      assertEquals("Employee4", assignment.assignments().get(4).employee().name());
      assertEquals("Employee5", assignment.assignments().get(5).employee().name());
      assertEquals("Employee6", assignment.assignments().get(6).employee().name());
      assertEquals("Employee7", assignment.assignments().get(7).employee().name());

      assertEquals(0, assignment.score(), "Score should be 0 since all are AVAILABLE");
    }

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
  }

  @Nested
  @DisplayName("[パフォーマンス]")
  class Performance {

    @Test
    @DisplayName("[H-1] Given: 12人全員が全枠◯のとき, When: assignを実行すると, Then: 10秒以内に完了する")
    void completesWithinTenSecondsForTwelveEmployees() {
      List<Employee> employees = createAllAvailableEmployees(12);
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      // Use assertTimeout with Duration to measure and enforce time limit
      org.junit.jupiter.api.Assertions.assertTimeout(
          Duration.ofSeconds(10),
          () -> {
            Optional<AssignmentResult> result = service.assign(employees);
            assertTrue(result.isPresent(), "Assignment should succeed for 12 employees");
          });
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
