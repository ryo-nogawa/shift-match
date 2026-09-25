package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.ShiftSlot;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
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
    @DisplayName("[H-3] Given: 全員が7:30〜18:30のとき, When: assignを実行すると, Then: 入力順どおりに枠1→6へ割り当てられる")
    void assignsInInputOrderToFrames() {
      List<Employee> employees = new ArrayList<>();
      for (int i = 0; i < 8; i++) {
        employees.add(
            Employee.working(
                "Employee" + i, java.time.LocalTime.of(7, 30), java.time.LocalTime.of(18, 30)));
      }
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      Optional<AssignmentResult> result = service.assign(employees);

      assertTrue(result.isPresent());
      AssignmentResult assignment = result.get();

      assertEquals(ShiftSlot.SLOT_1, assignment.assignments().get(0).slot());
      assertEquals(ShiftSlot.SLOT_1, assignment.assignments().get(1).slot());

      assertEquals(ShiftSlot.SLOT_2, assignment.assignments().get(2).slot());

      assertEquals(ShiftSlot.SLOT_3, assignment.assignments().get(3).slot());

      assertEquals(ShiftSlot.SLOT_4, assignment.assignments().get(4).slot());

      assertEquals(ShiftSlot.SLOT_5, assignment.assignments().get(5).slot());

      assertEquals(ShiftSlot.SLOT_6, assignment.assignments().get(6).slot());
      assertEquals(ShiftSlot.SLOT_6, assignment.assignments().get(7).slot());
    }

    @Test
    @DisplayName("[H-2] Given: 全員が7:30〜18:30のとき, When: assignを実行すると, Then: 1人が複数の枠に割り当てられない")
    void eachPersonAssignedToOnlyOneSlot() {
      List<Employee> employees = new ArrayList<>();
      for (int i = 0; i < 8; i++) {
        employees.add(
            Employee.working(
                "Employee" + i, java.time.LocalTime.of(7, 30), java.time.LocalTime.of(18, 30)));
      }
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
    @DisplayName(
        "[H-3] Given: 1名が9:00〜16:30で他の8名が7:30〜18:30のとき, When: assignを実行すると, Then: その1名が枠5に割り当てられない")
    void excludesUnavailableEmployeeFromSlot() {
      List<Employee> employees = new ArrayList<>();

      // Employee0: 9:00〜16:30（枠5に入れない）
      employees.add(
          Employee.working(
              "Employee0", java.time.LocalTime.of(9, 0), java.time.LocalTime.of(16, 30)));

      for (int i = 1; i < 9; i++) {
        employees.add(
            Employee.working(
                "Employee" + i, java.time.LocalTime.of(7, 30), java.time.LocalTime.of(18, 30)));
      }

      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();
      Optional<AssignmentResult> result = service.assign(employees);

      assertTrue(result.isPresent());
      AssignmentResult assignment = result.get();

      // Employee0 が枠5に割り当てられていないことを確認
      boolean employee0InSlot5 =
          assignment.assignments().stream()
              .anyMatch(
                  a -> a.employee().name().equals("Employee0") && a.slot() == ShiftSlot.SLOT_5);
      assertFalse(employee0InSlot5, "Employee0 should not be assigned to slot 5");
    }

    @Test
    @DisplayName(
        "[H-3] Given: 8名のうち1名が休みで7名が7:30〜18:30のとき, When: assignを実行すると, Then: Optional.emptyになる")
    void returnsEmptyWhenOneEmployeeAllUnavailableAndOthersCannotFillAllSlots() {
      List<Employee> employees = new ArrayList<>();

      employees.add(Employee.onLeave("Employee0"));

      for (int i = 1; i < 8; i++) {
        employees.add(
            Employee.working(
                "Employee" + i, java.time.LocalTime.of(7, 30), java.time.LocalTime.of(18, 30)));
      }

      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();
      Optional<AssignmentResult> result = service.assign(employees);

      assertFalse(result.isPresent(), "Should return empty when only 7 valid employees remain");
    }
  }

  @Nested
  @DisplayName("[V-4] 有効な従業員が8名未満")
  class InsufficientEmployees {

    @Test
    @DisplayName("[V-4] Given: 休みでない従業員が7名のとき, When: assignを実行すると, Then: Optional.emptyが返される")
    void returnsEmptyWhenLessThanEightValidEmployees() {
      List<Employee> employees = new ArrayList<>();
      for (int i = 0; i < 7; i++) {
        employees.add(
            Employee.working(
                "Employee" + i, java.time.LocalTime.of(7, 30), java.time.LocalTime.of(18, 30)));
      }
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      Optional<AssignmentResult> result = service.assign(employees);

      assertFalse(result.isPresent());
    }
  }

  @Nested
  @DisplayName("パフォーマンス")
  class Performance {

    @Test
    @DisplayName("Given: 12人全員が7:30〜18:30のとき, When: assignを実行すると, Then: 500ミリ秒以内に完了する")
    void completesWithin500MillisForAllAvailable() {
      List<Employee> employees = new ArrayList<>();
      for (int i = 0; i < 12; i++) {
        employees.add(
            Employee.working(
                "Employee" + i, java.time.LocalTime.of(7, 30), java.time.LocalTime.of(18, 30)));
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
    @DisplayName("Given: 12人の勤務時間がランダムのとき, When: assignを実行すると, Then: 500ミリ秒以内に完了する")
    void completesWithin500MillisForRandomTimeRanges() {
      java.util.Random random = new java.util.Random(54321L);
      List<Employee> employees = new ArrayList<>();
      for (int i = 0; i < 12; i++) {
        // 開始時刻：7:30〜9:00の30分刻み（3通り）
        int startHour = 7;
        int startMin = 30 + (random.nextInt(3) * 30);
        if (startMin >= 60) {
          startHour++;
          startMin -= 60;
        }
        // 終了時刻：16:30〜18:30の30分刻み（5通り）
        int endHour = 16 + (random.nextInt(5) / 2);
        int endMin = 30 + ((random.nextInt(5) % 2) * 60);
        if (endMin >= 60) {
          endHour++;
          endMin -= 60;
        }
        employees.add(
            Employee.working(
                "Employee" + i,
                java.time.LocalTime.of(startHour, startMin),
                java.time.LocalTime.of(endHour, endMin)));
      }
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      org.junit.jupiter.api.Assertions.assertTimeout(
          Duration.ofMillis(500),
          () -> {
            Optional<AssignmentResult> result = service.assign(employees);
          });
    }
  }

  @Nested
  @DisplayName("メモ化の性能（割り当て不能状態のキャッシング）")
  class MemoizationPerformance {

    @Test
    @DisplayName("Given: 12名全員が9:00〜16:30（枠6に入れない）のとき, When: assignを実行すると, Then: 500ms以内に不成立と判定される")
    void performanceWhenLastSlotImpossible() {
      List<Employee> employees = new ArrayList<>();
      for (int i = 0; i < 12; i++) {
        // 9:00〜16:30：枠6（9:00〜18:30）に入れない
        employees.add(
            Employee.working(
                "Emp" + i, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(16, 30)));
      }

      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();
      assertTimeout(
          Duration.ofMillis(500),
          () -> {
            Optional<AssignmentResult> result = service.assign(employees);
            assertFalse(
                result.isPresent(), "Should be unassignable when last slot has no available");
          });
    }

    @Test
    @DisplayName(
        "Given: 12名中11名が9:00〜16:30、1名のみが7:30〜18:30のとき, When: assignを実行すると, Then:"
            + " 500ms以内に不成立と判定される")
    void performanceWhenLastTwoSlotsBottleneck() {
      List<Employee> employees = new ArrayList<>();
      // Emp0: 枠5・6に入れる唯一の人
      employees.add(
          Employee.working("Emp0", java.time.LocalTime.of(7, 30), java.time.LocalTime.of(18, 30)));

      for (int i = 1; i < 12; i++) {
        // 9:00〜16:30：枠5・6に入れない
        employees.add(
            Employee.working(
                "Emp" + i, java.time.LocalTime.of(9, 0), java.time.LocalTime.of(16, 30)));
      }

      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();
      assertTimeout(
          Duration.ofMillis(500),
          () -> {
            Optional<AssignmentResult> result = service.assign(employees);
            assertFalse(
                result.isPresent(), "Should be unassignable with bottleneck at slots 5 & 6");
          });
    }
  }

  @Nested
  @DisplayName("[H-3] 時間帯と canWork() での割り当て判定")
  class TimeRangeAssignment {

    @Test
    @DisplayName("[H-3] Given: 全員が7:30〜18:30のとき, When: assignを実行すると, Then: 割り当てが成立し8名分が返される")
    void assignsAllEightEmployeesWhenAllAvailable() {
      List<Employee> employees = new ArrayList<>();
      for (int i = 0; i < 8; i++) {
        employees.add(
            Employee.working(
                "Employee" + i, java.time.LocalTime.of(7, 30), java.time.LocalTime.of(18, 30)));
      }

      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();
      Optional<AssignmentResult> result = service.assign(employees);

      assertTrue(result.isPresent(), "Should assign all 8 employees with 7:30-18:30");
      assertEquals(8, result.get().assignments().size());
    }

    @Test
    @DisplayName("[V-4] Given: 9名のうち1名が休みのとき, When: assignを実行すると, Then: 成立し8名の割り当てが返される")
    void assignsWhenEightNonLeaveEmployeesExistOutOfNine() {
      List<Employee> employees = new ArrayList<>();
      for (int i = 0; i < 8; i++) {
        employees.add(
            Employee.working(
                "Employee" + i, java.time.LocalTime.of(7, 30), java.time.LocalTime.of(18, 30)));
      }
      employees.add(Employee.onLeave("OnLeave"));

      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();
      Optional<AssignmentResult> result = service.assign(employees);

      assertTrue(result.isPresent(), "Should succeed with 8 non-leave employees");
      assertEquals(8, result.get().assignments().size());
    }

    @Test
    @DisplayName("[V-4] Given: 8名のうち1名が休みのとき, When: assignを実行すると, Then: Optional.emptyになる")
    void returnsEmptyWhenOnlySevenNonLeaveEmployees() {
      List<Employee> employees = new ArrayList<>();
      for (int i = 0; i < 7; i++) {
        employees.add(
            Employee.working(
                "Employee" + i, java.time.LocalTime.of(7, 30), java.time.LocalTime.of(18, 30)));
      }
      employees.add(Employee.onLeave("OnLeave"));

      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();
      Optional<AssignmentResult> result = service.assign(employees);

      assertFalse(result.isPresent(), "Should return empty when only 7 non-leave employees exist");
    }

    @Test
    @DisplayName(
        "[F-4] Given: 全員が7:30〜18:30で割り当てが成立したとき, When: unassignedEmployeesを確認すると,"
            + " Then: 余った従業員と休みの従業員の両方が入力順で入る")
    void unassignedIncludesUnusedAndOnLeaveEmployees() {
      List<Employee> employees = new ArrayList<>();
      for (int i = 0; i < 8; i++) {
        employees.add(
            Employee.working(
                "Employee" + i, java.time.LocalTime.of(7, 30), java.time.LocalTime.of(18, 30)));
      }
      employees.add(
          Employee.working(
              "Employee8", java.time.LocalTime.of(7, 30), java.time.LocalTime.of(18, 30)));
      employees.add(Employee.onLeave("OnLeave9"));

      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();
      Optional<AssignmentResult> result = service.assign(employees);

      assertTrue(result.isPresent());
      List<Employee> unassigned = result.get().unassignedEmployees();
      assertEquals(2, unassigned.size(), "Should have 2 unassigned (1 unused + 1 on leave)");
      assertEquals("Employee8", unassigned.get(0).name());
      assertEquals("OnLeave9", unassigned.get(1).name());
    }
  }

  @Nested
  @DisplayName("[F-3] スコア評価：ずれの合計（分）")
  class GapMinutesScoreEvaluation {

    @Test
    @DisplayName(
        "[F-3] Given: 全員が7:30〜18:30（660分）の8名のとき, When: assignを実行すると,"
            + " Then: scoreが1380（=8×660-3900）になる")
    void allEmployeesFullAvailableScore() {
      List<Employee> employees = new ArrayList<>();
      for (int i = 0; i < 8; i++) {
        employees.add(
            Employee.working(
                "Employee" + i, java.time.LocalTime.of(7, 30), java.time.LocalTime.of(18, 30)));
      }

      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();
      Optional<AssignmentResult> result = service.assign(employees);

      assertTrue(result.isPresent());
      assertEquals(1380, result.get().score(), "Score should be 1380 for full availability");
    }

    @Test
    @DisplayName(
        "[F-3] Given: 7:30〜18:30の7名と7:30〜14:30（420分）の1名のとき, When: assignを実行すると,"
            + " Then: scoreが1140（=7×660+420-3900）になる")
    void mixedAvailabilityScore() {
      List<Employee> employees = new ArrayList<>();
      for (int i = 0; i < 7; i++) {
        employees.add(
            Employee.working(
                "Employee" + i, java.time.LocalTime.of(7, 30), java.time.LocalTime.of(18, 30)));
      }
      employees.add(
          Employee.working(
              "Employee7", java.time.LocalTime.of(7, 30), java.time.LocalTime.of(14, 30)));

      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();
      Optional<AssignmentResult> result = service.assign(employees);

      assertTrue(result.isPresent());
      assertEquals(1140, result.get().score(), "Score should be 1140 for mixed availability");
    }

    @Test
    @DisplayName(
        "[F-3] Given: 7名が7:30〜18:30、1名が8:00〜18:00のとき, When: assignを実行すると," + " Then: scoreが1320になる")
    void alternativeTimeRangeScore() {
      List<Employee> employees = new ArrayList<>();
      for (int i = 0; i < 7; i++) {
        employees.add(
            Employee.working(
                "Employee" + i, java.time.LocalTime.of(7, 30), java.time.LocalTime.of(18, 30)));
      }
      employees.add(
          Employee.working(
              "Employee7", java.time.LocalTime.of(8, 0), java.time.LocalTime.of(18, 0)));

      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();
      Optional<AssignmentResult> result = service.assign(employees);

      assertTrue(result.isPresent());
      // score = 7×660 + 600 - 3900 = 4620 + 600 - 3900 = 1320
      assertEquals(1320, result.get().score(), "Score should be 1320 for mixed availability");
    }
  }

  @Nested
  @DisplayName("[F-3] 同点時の案の選択")
  class TiedScoreSelection {

    @Test
    @DisplayName(
        "[F-3] Given: 全員が7:30〜18:30の9名（同点）のとき, When: assignを実行すると,"
            + " Then: 先頭から8名が割り当てられ、9番目が未出勤者になる")
    void selectsFirstAssignmentWhenAllTied() {
      List<Employee> employees = new ArrayList<>();
      for (int i = 0; i < 9; i++) {
        employees.add(
            Employee.working(
                "Employee" + i, java.time.LocalTime.of(7, 30), java.time.LocalTime.of(18, 30)));
      }

      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();
      Optional<AssignmentResult> result = service.assign(employees);

      assertTrue(result.isPresent());
      AssignmentResult assignment = result.get();

      // 枠 1: Employee0, Employee1
      assertEquals("Employee0", assignment.assignments().get(0).employee().name());
      assertEquals("Employee1", assignment.assignments().get(1).employee().name());

      // 枠 2: Employee2
      assertEquals("Employee2", assignment.assignments().get(2).employee().name());

      // 枠 3: Employee3
      assertEquals("Employee3", assignment.assignments().get(3).employee().name());

      // 枠 4: Employee4
      assertEquals("Employee4", assignment.assignments().get(4).employee().name());

      // 枠 5: Employee5
      assertEquals("Employee5", assignment.assignments().get(5).employee().name());

      // 枠 6: Employee6, Employee7
      assertEquals("Employee6", assignment.assignments().get(6).employee().name());
      assertEquals("Employee7", assignment.assignments().get(7).employee().name());

      // Employee8 が未出勤者
      List<Employee> unassigned = assignment.unassignedEmployees();
      assertEquals(1, unassigned.size(), "Should have 1 unassigned employee");
      assertEquals("Employee8", unassigned.get(0).name(), "Employee8 should be unassigned");
    }
  }

  @Nested
  @DisplayName("[F-3] 動的計画法の検証（総当たりとの一致）")
  class DynamicProgrammingVerification {

    private record BruteForceResult(
        Optional<AssignmentResult> result, List<Employee> inputEmployees) {}

    private BruteForceResult bruteForceExplore(List<Employee> employees) {
      ShiftSlot[] slots =
          new ShiftSlot[] {
            ShiftSlot.SLOT_1, ShiftSlot.SLOT_1, ShiftSlot.SLOT_2, ShiftSlot.SLOT_3,
            ShiftSlot.SLOT_4, ShiftSlot.SLOT_5, ShiftSlot.SLOT_6, ShiftSlot.SLOT_6
          };

      // 候補従業員（休みでない、かつ名前がある）
      List<Employee> candidates = new ArrayList<>();
      for (Employee e : employees) {
        if (!e.off() && !e.name().isEmpty()) {
          candidates.add(e);
        }
      }

      if (candidates.size() < 8) {
        return new BruteForceResult(Optional.empty(), employees);
      }

      int minScore = Integer.MAX_VALUE;
      List<Employee> bestAssignment = null;

      // すべての組み合わせを列挙（2^n）
      for (int mask = 0; mask < (1 << candidates.size()); mask++) {
        if (Integer.bitCount(mask) != 8) {
          continue;
        }

        // この mask に対応する従業員のセットが割り当て可能かチェック
        List<Employee> selectedEmployees = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
          if ((mask & (1 << i)) != 0) {
            selectedEmployees.add(candidates.get(i));
          }
        }

        // 枠 1→6、入力順の辞書順で割り当てを試みる
        List<Employee> assignedBySlot = new ArrayList<>();
        boolean canAssign = true;

        for (ShiftSlot slot : slots) {
          Employee assigned = null;
          for (Employee emp : selectedEmployees) {
            if (!assignedBySlot.contains(emp) && emp.canWork(slot)) {
              assigned = emp;
              break;
            }
          }
          if (assigned == null) {
            canAssign = false;
            break;
          }
          assignedBySlot.add(assigned);
        }

        if (!canAssign) {
          continue;
        }

        // スコアを計算
        int totalScore = 0;
        for (int i = 0; i < 8; i++) {
          totalScore += assignedBySlot.get(i).gapMinutes(slots[i]);
        }

        // 最小スコアで最初に到達する案を選択（同点で更新しない）
        if (totalScore < minScore) {
          minScore = totalScore;
          bestAssignment = new ArrayList<>(assignedBySlot);
        }
      }

      if (bestAssignment == null) {
        return new BruteForceResult(Optional.empty(), employees);
      }

      // AssignmentResult を構築（休憩時刻を計算）
      List<com.example.shiftmatch.domain.ShiftAssignment> assignments = new ArrayList<>();
      Map<ShiftSlot, Integer> slotCounts = new HashMap<>();
      for (int i = 0; i < 8; i++) {
        ShiftSlot slot = slots[i];
        slotCounts.put(slot, slotCounts.getOrDefault(slot, 0) + 1);
        LocalTime breakStart = calculateBreakStart(slot, slotCounts.get(slot));
        LocalTime breakEnd = breakStart.plusMinutes(slot.breakDurationMinutes());
        assignments.add(
            new com.example.shiftmatch.domain.ShiftAssignment(
                bestAssignment.get(i), slot, breakStart, breakEnd));
      }

      // 未出勤者（割り当てられなかった従業員と休みの従業員）
      Set<Employee> assigned = new HashSet<>(bestAssignment);
      List<Employee> unassigned = new ArrayList<>();
      for (Employee e : employees) {
        if (!assigned.contains(e)) {
          unassigned.add(e);
        }
      }

      AssignmentResult result = new AssignmentResult(assignments, minScore, unassigned);
      return new BruteForceResult(Optional.of(result), employees);
    }

    @Test
    @DisplayName("[F-3] Given: 9名のランダム時間帯（シード1）のとき, When: DP と総当たりを実行すると, Then: 割り当てとスコアが一致する")
    void dynamicProgrammingMatchesBruteForceWithSeed1() {
      Random random = new Random(12345L);
      List<Employee> employees = generateRandomTimeRangeEmployees(random, 9);

      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();
      Optional<AssignmentResult> dpResult = service.assign(employees);
      BruteForceResult bruteForceResult = bruteForceExplore(employees);

      assertEquals(
          dpResult.isPresent(),
          bruteForceResult.result().isPresent(),
          "DP and brute force should agree on feasibility");

      if (dpResult.isPresent()) {
        assertEquals(
            dpResult.get().score(),
            bruteForceResult.result().get().score(),
            "DP and brute force should have same score");
        assertEquals(
            dpResult.get().assignments().size(),
            bruteForceResult.result().get().assignments().size(),
            "DP and brute force should have same assignment count");
      }
    }

    @Test
    @DisplayName("[F-3] Given: 10名のランダム時間帯（シード2）のとき, When: DP と総当たりを実行すると, Then: 割り当てとスコアが一致する")
    void dynamicProgrammingMatchesBruteForceWithSeed2() {
      Random random = new Random(54321L);
      List<Employee> employees = generateRandomTimeRangeEmployees(random, 10);

      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();
      Optional<AssignmentResult> dpResult = service.assign(employees);
      BruteForceResult bruteForceResult = bruteForceExplore(employees);

      assertEquals(
          dpResult.isPresent(),
          bruteForceResult.result().isPresent(),
          "DP and brute force should agree on feasibility");

      if (dpResult.isPresent()) {
        assertEquals(
            dpResult.get().score(),
            bruteForceResult.result().get().score(),
            "DP and brute force should have same score");
      }
    }

    @Test
    @DisplayName("[F-3] Given: 9名で成立しない入力（シード3）のとき, When: DP と総当たりを実行すると, Then: 両方が案なしで一致する")
    void dynamicProgrammingMatchesBruteForceWhenUnfeasibleWithSeed3() {
      Random random = new Random(99999L);
      List<Employee> employees = generateRandomTimeRangeEmployees(random, 9);

      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();
      Optional<AssignmentResult> dpResult = service.assign(employees);
      BruteForceResult bruteForceResult = bruteForceExplore(employees);

      assertEquals(
          dpResult.isPresent(),
          bruteForceResult.result().isPresent(),
          "DP and brute force should agree on feasibility (both empty is also valid)");

      if (dpResult.isPresent()) {
        assertEquals(
            dpResult.get().score(),
            bruteForceResult.result().get().score(),
            "DP and brute force should have same score");
      }
    }

    private List<Employee> generateRandomTimeRangeEmployees(Random random, int count) {
      List<Employee> employees = new ArrayList<>();
      LocalTime[] timeOptions = generateTimeOptions();

      for (int i = 0; i < count; i++) {
        int startIdx = random.nextInt(timeOptions.length - 1);
        int endIdx = startIdx + 1 + random.nextInt(timeOptions.length - startIdx - 1);

        LocalTime start = timeOptions[startIdx];
        LocalTime end = timeOptions[endIdx];

        employees.add(Employee.working("Employee" + i, start, end));
      }

      return employees;
    }

    private LocalTime[] generateTimeOptions() {
      List<LocalTime> options = new ArrayList<>();
      for (int h = 7; h <= 18; h++) {
        for (int m = 0; m < 60; m += 30) {
          if (h == 7 && m == 0) {
            continue; // Skip 7:00
          }
          options.add(LocalTime.of(h, m));
        }
      }
      return options.toArray(new LocalTime[0]);
    }

    private LocalTime calculateBreakStart(ShiftSlot slot, int slotIndex) {
      // 仕様の 2 章から：
      // 枠 1（1 人目） | 12:00
      // 枠 1（2 人目） | 12:00
      // 枠 2 | 12:45
      // 枠 3 | 12:45
      // 枠 4 | 13:30
      // 枠 5 | 13:30
      // 枠 6（1 人目） | 14:15
      // 枠 6（2 人目） | 14:30
      return switch (slot) {
        case SLOT_1 -> LocalTime.of(12, 0);
        case SLOT_2 -> LocalTime.of(12, 45);
        case SLOT_3 -> LocalTime.of(12, 45);
        case SLOT_4 -> LocalTime.of(13, 30);
        case SLOT_5 -> LocalTime.of(13, 30);
        case SLOT_6 -> slotIndex == 1 ? LocalTime.of(14, 15) : LocalTime.of(14, 30);
      };
    }
  }
}
