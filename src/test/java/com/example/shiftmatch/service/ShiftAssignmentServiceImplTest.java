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
    @DisplayName(
        "[H-1] Given: 8人全員が全6枠でAVAILABLEのとき, When: assignを実行すると, Then: 入力順どおりに枠1→6へ割り当てられる")
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
    @DisplayName("[V-4] Given: 有効な従業員が7名のとき, When: assignを実行すると, Then: Optional.emptyが返される")
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
        "[H-1] Given: ある従業員だけが枠1に◎を付けたとき, When: assignを実行すると, Then: その従業員が枠1に割り当てられ、scoreが1になる")
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
        "[H-1] Given: 全員が全枠◎を付けずに◯で、スコア0のとき, When: assignを実行すると, Then: 入力順どおりに枠1→6へ割り当てられる")
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
    @DisplayName("[H-1] Given: ◎を付けた従業員数が異なる複数の案が存在するとき, When: assignを実行すると, Then: ◎の数が最も多い案が選ばれる")
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
    @DisplayName("[H-1] Given: 12人全員が全枠◯のとき, When: assignを実行すると, Then: 500ミリ秒以内に完了する")
    void completesWithin500MillisForAllAvailable() {
      List<Employee> employees = createAllAvailableEmployees(12);
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      org.junit.jupiter.api.Assertions.assertTimeout(
          Duration.ofMillis(500),
          () -> {
            Optional<AssignmentResult> result = service.assign(employees);
            assertTrue(result.isPresent(), "Assignment should succeed for 12 employees");
          });
    }

    @Test
    @DisplayName("[H-1] Given: 12人全員が全枠◎のとき, When: assignを実行すると, Then: 500ミリ秒以内に完了する")
    void completesWithin500MillisForAllDesired() {
      List<Employee> employees = new ArrayList<>();
      for (int i = 0; i < 12; i++) {
        employees.add(
            new Employee(
                "Employee" + i,
                List.of(
                    Wish.DESIRED,
                    Wish.DESIRED,
                    Wish.DESIRED,
                    Wish.DESIRED,
                    Wish.DESIRED,
                    Wish.DESIRED)));
      }
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      org.junit.jupiter.api.Assertions.assertTimeout(
          Duration.ofMillis(500),
          () -> {
            Optional<AssignmentResult> result = service.assign(employees);
            assertTrue(result.isPresent(), "Assignment should succeed for 12 employees");
          });
    }

    @Test
    @DisplayName("[H-1] Given: 12人の希望がすべてランダムのとき, When: assignを実行すると, Then: 500ミリ秒以内に完了する")
    void completesWithin500MillisForRandomWishes() {
      java.util.Random random = new java.util.Random(54321L);
      List<Employee> employees = new ArrayList<>();
      for (int i = 0; i < 12; i++) {
        List<Wish> wishes = new ArrayList<>();
        for (int j = 0; j < 6; j++) {
          int val = random.nextInt(3);
          if (val == 0) {
            wishes.add(Wish.DESIRED);
          } else if (val == 1) {
            wishes.add(Wish.AVAILABLE);
          } else {
            wishes.add(Wish.UNAVAILABLE);
          }
        }
        employees.add(new Employee("Employee" + i, wishes));
      }
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      org.junit.jupiter.api.Assertions.assertTimeout(
          Duration.ofMillis(500),
          () -> {
            Optional<AssignmentResult> result = service.assign(employees);
            // Assignment may or may not exist, but should complete quickly
          });
    }
  }

  @Nested
  @DisplayName("[動的計画法への置き換え検証]")
  class DynamicProgrammingVerification {

    @Test
    @DisplayName(
        "[H-1][H-2][H-3] Given: 9～10人のランダムな希望の入力200通り, When: assignを実行すると, Then: 参照実装の結果と一致する")
    void dynamicProgrammingMatchesBruteForceReference() {
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();
      java.util.Random random = new java.util.Random(12345L); // 固定シード
      int testCases = 200;

      for (int testNum = 0; testNum < testCases; testNum++) {
        int employeeCount = 9 + random.nextInt(2); // 9 or 10
        List<Employee> employees = generateRandomEmployees(random, employeeCount);

        Optional<AssignmentResult> actual = service.assign(employees);
        Optional<AssignmentResult> expected = bruteForceReference(employees);

        assertEquals(
            expected.isPresent(),
            actual.isPresent(),
            "Test case " + testNum + ": presence should match");

        if (expected.isPresent() && actual.isPresent()) {
          AssignmentResult expectedResult = expected.get();
          AssignmentResult actualResult = actual.get();

          assertEquals(
              expectedResult.score(),
              actualResult.score(),
              "Test case " + testNum + ": score should match");

          assertEquals(
              expectedResult.assignments().size(),
              actualResult.assignments().size(),
              "Test case " + testNum + ": assignment count should match");

          for (int i = 0; i < expectedResult.assignments().size(); i++) {
            var expectedAssignment = expectedResult.assignments().get(i);
            var actualAssignment = actualResult.assignments().get(i);

            assertEquals(
                expectedAssignment.employee().name(),
                actualAssignment.employee().name(),
                "Test case " + testNum + ", position " + i + ": employee name should match");

            assertEquals(
                expectedAssignment.slot(),
                actualAssignment.slot(),
                "Test case " + testNum + ", position " + i + ": slot should match");
          }
        }
      }
    }
  }

  private List<Employee> generateRandomEmployees(java.util.Random random, int count) {
    List<Employee> employees = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      List<Wish> wishes = new ArrayList<>();
      for (int j = 0; j < 6; j++) {
        int val = random.nextInt(100);
        // 70% UNAVAILABLE, 20% AVAILABLE, 10% DESIRED（不成立も含める）
        if (val < 70) {
          wishes.add(Wish.UNAVAILABLE);
        } else if (val < 90) {
          wishes.add(Wish.AVAILABLE);
        } else {
          wishes.add(Wish.DESIRED);
        }
      }
      employees.add(new Employee("Emp" + i, wishes));
    }
    return employees;
  }

  private Optional<AssignmentResult> bruteForceReference(List<Employee> employees) {
    // V-1: Filter out blank names
    List<Employee> validEmployees =
        employees.stream().filter(emp -> emp.name() != null && !emp.name().isBlank()).toList();

    // V-4: Check minimum employees
    if (validEmployees.size() < 8) {
      return Optional.empty();
    }

    // Brute force: enumerate all permutations and find the first one with max score
    Wish[][] wishes = new Wish[validEmployees.size()][6];
    for (int i = 0; i < validEmployees.size(); i++) {
      List<Wish> employeeWishes = validEmployees.get(i).wishes();
      for (int j = 0; j < 6; j++) {
        wishes[i][j] = employeeWishes.get(j);
      }
    }

    BruteForceResult bestResult = new BruteForceResult();
    boolean[] used = new boolean[validEmployees.size()];
    int[] assignment = new int[8];

    bruteForceExplore(validEmployees, wishes, used, assignment, 0, 0, bestResult);

    if (bestResult.assignment == null) {
      return Optional.empty();
    }

    // Build the result
    return buildResultFromAssignment(validEmployees, bestResult.assignment);
  }

  private static class BruteForceResult {
    int[] assignment;
    int score;

    BruteForceResult() {
      this.assignment = null;
      this.score = -1;
    }
  }

  private void bruteForceExplore(
      List<Employee> employees,
      Wish[][] wishes,
      boolean[] used,
      int[] assignment,
      int assignmentIndex,
      int slotIndex,
      BruteForceResult bestResult) {

    if (slotIndex >= ShiftSlot.values().length) {
      // Calculate score
      int score = 0;
      int pos = 0;
      for (int s = 0; s < ShiftSlot.values().length; s++) {
        ShiftSlot slot = ShiftSlot.values()[s];
        int count = slot.numberOfEmployees();
        for (int i = 0; i < count; i++) {
          int idx = assignment[pos];
          if (wishes[idx][s] == Wish.DESIRED) {
            score++;
          }
          pos++;
        }
      }

      // Use > (not >=) to keep the first maximum
      if (score > bestResult.score) {
        bestResult.score = score;
        bestResult.assignment = assignment.clone();
      }
      return;
    }

    ShiftSlot slot = ShiftSlot.values()[slotIndex];
    int requiredCount = slot.numberOfEmployees();

    bruteForceExploreSlot(
        employees,
        wishes,
        used,
        assignment,
        assignmentIndex,
        0,
        requiredCount,
        slotIndex,
        bestResult);
  }

  private void bruteForceExploreSlot(
      List<Employee> employees,
      Wish[][] wishes,
      boolean[] used,
      int[] assignment,
      int assignmentIndex,
      int candidateStart,
      int requiredCount,
      int slotIndex,
      BruteForceResult bestResult) {

    if (requiredCount == 0) {
      bruteForceExplore(
          employees, wishes, used, assignment, assignmentIndex, slotIndex + 1, bestResult);
      return;
    }

    for (int i = candidateStart; i < employees.size(); i++) {
      if (used[i]) {
        continue;
      }

      if (wishes[i][slotIndex] == Wish.UNAVAILABLE) {
        continue;
      }

      used[i] = true;
      assignment[assignmentIndex] = i;

      bruteForceExploreSlot(
          employees,
          wishes,
          used,
          assignment,
          assignmentIndex + 1,
          i + 1,
          requiredCount - 1,
          slotIndex,
          bestResult);

      used[i] = false;
    }
  }

  private Optional<com.example.shiftmatch.domain.AssignmentResult> buildResultFromAssignment(
      List<Employee> employees, int[] assignment) {
    com.example.shiftmatch.domain.BreakScheduler scheduler =
        new com.example.shiftmatch.domain.BreakScheduler();
    ShiftSlot[] slots = ShiftSlot.values();
    List<ShiftSlot> slotList = new ArrayList<>();

    int position = 0;
    for (int slotIndex = 0; slotIndex < slots.length; slotIndex++) {
      ShiftSlot slot = slots[slotIndex];
      int requiredCount = slot.numberOfEmployees();
      for (int i = 0; i < requiredCount; i++) {
        slotList.add(slot);
        position++;
      }
    }

    List<com.example.shiftmatch.domain.BreakInterval> breaks = scheduler.schedule(slotList);

    List<com.example.shiftmatch.domain.ShiftAssignment> shiftAssignments = new ArrayList<>();
    int score = 0;
    boolean[] used = new boolean[employees.size()];

    for (int i = 0; i < assignment.length; i++) {
      int employeeIndex = assignment[i];
      Employee employee = employees.get(employeeIndex);
      ShiftSlot slot = slotList.get(i);
      com.example.shiftmatch.domain.BreakInterval breakInterval = breaks.get(i);

      shiftAssignments.add(
          new com.example.shiftmatch.domain.ShiftAssignment(
              employee, slot, breakInterval.startTime(), breakInterval.endTime()));

      int slotIndex = getSlotIndex(slot);
      if (employee.wishes().get(slotIndex) == Wish.DESIRED) {
        score++;
      }

      used[employeeIndex] = true;
    }

    List<Employee> unassigned = new ArrayList<>();
    for (int i = 0; i < employees.size(); i++) {
      if (!used[i]) {
        unassigned.add(employees.get(i));
      }
    }

    return Optional.of(new AssignmentResult(shiftAssignments, score, unassigned));
  }

  private int getSlotIndex(ShiftSlot slot) {
    ShiftSlot[] slots = ShiftSlot.values();
    for (int i = 0; i < slots.length; i++) {
      if (slots[i] == slot) {
        return i;
      }
    }
    throw new IllegalStateException("Slot index not found");
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
