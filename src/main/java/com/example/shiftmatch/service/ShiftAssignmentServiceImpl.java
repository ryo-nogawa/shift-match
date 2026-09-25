package com.example.shiftmatch.service;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.BreakInterval;
import com.example.shiftmatch.domain.BreakScheduler;
import com.example.shiftmatch.domain.DuplicateNameError;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.ShiftAssignment;
import com.example.shiftmatch.domain.ShiftSlot;
import com.example.shiftmatch.domain.Wish;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * シフト割り当てを行うサービス実装。
 *
 * <p>6 種類の枠に 8 名を割り当てます。動的計画法で最適な案を高速に求め、条件を満たす案の
 * 中でスコアが最大かつ入力順で最初の案を返します。
 */
@Service
public class ShiftAssignmentServiceImpl implements ShiftAssignmentService {

  private static final int UNCOMPUTED = -2;
  private static final int IMPOSSIBLE = -1;

  @Override
  public Optional<AssignmentResult> assign(List<Employee> employees) {
    List<Employee> validEmployees =
        employees.stream().filter(emp -> emp.name() != null && !emp.name().isBlank()).toList();

    if (validEmployees.size() < ShiftSlot.totalEmployees()) {
      return Optional.empty();
    }

    Wish[][] wishes = new Wish[validEmployees.size()][ShiftSlot.values().length];
    for (int i = 0; i < validEmployees.size(); i++) {
      List<Wish> employeeWishes = validEmployees.get(i).wishes();
      for (int j = 0; j < ShiftSlot.values().length; j++) {
        wishes[i][j] = employeeWishes.get(j);
      }
    }

    int n = validEmployees.size();
    int[][] memo = new int[ShiftSlot.values().length + 1][1 << n];
    for (int i = 0; i < ShiftSlot.values().length + 1; i++) {
      for (int j = 0; j < (1 << n); j++) {
        memo[i][j] = UNCOMPUTED;
      }
    }

    int maxScore = computeMaxScore(wishes, 0, 0, memo);

    if (maxScore == IMPOSSIBLE) {
      return Optional.empty();
    }

    // 復元：入力順の辞書順で最初の最大スコア案を構築
    int[] assignment = new int[ShiftSlot.totalEmployees()];
    reconstructAssignment(wishes, 0, 0, maxScore, assignment, 0, memo);

    return Optional.of(buildResult(validEmployees, assignment));
  }

  /**
   * 動的計画法で最大スコアを計算します。
   *
   * @param wishes 従業員 × 枠の希望配列
   * @param slotIndex 現在の枠インデックス
   * @param usedMask 使用済み従業員のビットマスク
   * @param memo メモ化テーブル
   * @return 枠 slotIndex 以降で得られる最大の追加スコア（割り当て不可なら {@code IMPOSSIBLE}）
   */
  private int computeMaxScore(Wish[][] wishes, int slotIndex, int usedMask, int[][] memo) {
    if (slotIndex >= ShiftSlot.values().length) {
      return 0;
    }

    if (memo[slotIndex][usedMask] != UNCOMPUTED) {
      return memo[slotIndex][usedMask];
    }

    ShiftSlot slot = ShiftSlot.values()[slotIndex];
    int requiredCount = slot.numberOfEmployees();
    int maxScore = IMPOSSIBLE;

    // この枠に割り当てる従業員の組を列挙（入力順の辞書順）
    for (int combo : generateCombinations(wishes, slotIndex, usedMask, requiredCount)) {
      int nextMask = usedMask;
      int comboScore = 0;

      for (int i = 0, bit = 0; i < wishes.length && i < 32; i++) {
        if ((combo & (1 << i)) != 0) {
          nextMask |= (1 << i);
          if (wishes[i][slotIndex] == Wish.DESIRED) {
            comboScore++;
          }
          bit++;
          if (bit >= requiredCount) {
            break;
          }
        }
      }

      int futureScore = computeMaxScore(wishes, slotIndex + 1, nextMask, memo);
      if (futureScore != IMPOSSIBLE) {
        int totalScore = comboScore + futureScore;
        if (maxScore == IMPOSSIBLE || totalScore > maxScore) {
          maxScore = totalScore;
        }
      }
    }

    memo[slotIndex][usedMask] = maxScore;
    return maxScore;
  }

  /**
   * 指定された枠に割り当て可能な従業員の組み合わせを列挙します。
   *
   * @param wishes 従業員 × 枠の希望配列
   * @param slotIndex 枠インデックス
   * @param usedMask 使用済み従業員のビットマスク
   * @param requiredCount この枠に必要な人数
   * @return 組み合わせのリスト（各要素は従業員インデックスを示すビットマスク）
   */
  private java.util.List<Integer> generateCombinations(
      Wish[][] wishes, int slotIndex, int usedMask, int requiredCount) {
    java.util.List<Integer> combinations = new ArrayList<>();
    combinationHelper(wishes, slotIndex, usedMask, requiredCount, 0, 0, 0, combinations);
    return combinations;
  }

  /**
   * 組み合わせを再帰的に生成します。
   */
  private void combinationHelper(
      Wish[][] wishes,
      int slotIndex,
      int usedMask,
      int requiredCount,
      int currentIndex,
      int currentMask,
      int count,
      java.util.List<Integer> combinations) {
    if (count == requiredCount) {
      combinations.add(currentMask);
      return;
    }

    for (int i = currentIndex; i < wishes.length; i++) {
      if ((usedMask & (1 << i)) == 0 && wishes[i][slotIndex] != Wish.UNAVAILABLE) {
        combinationHelper(
            wishes,
            slotIndex,
            usedMask,
            requiredCount,
            i + 1,
            currentMask | (1 << i),
            count + 1,
            combinations);
      }
    }
  }

  /**
   * 復元：入力順の辞書順で最初の最大スコア案を構築します。
   *
   * <p>仕様 5.3 節の「列挙順で最初に最大スコアへ到達する案」と同じ案を得るため、 入力順の辞書順で組を列挙し、スコア条件を満たす最初の組を選びます。
   */
  private void reconstructAssignment(
      Wish[][] wishes,
      int slotIndex,
      int usedMask,
      int targetScore,
      int[] assignment,
      int assignmentIndex,
      int[][] memo) {
    if (slotIndex >= ShiftSlot.values().length) {
      return;
    }

    ShiftSlot slot = ShiftSlot.values()[slotIndex];
    int requiredCount = slot.numberOfEmployees();

    for (int combo : generateCombinations(wishes, slotIndex, usedMask, requiredCount)) {
      int nextMask = usedMask;
      int comboScore = 0;

      int assignedCount = 0;
      for (int i = 0; i < wishes.length && assignedCount < requiredCount; i++) {
        if ((combo & (1 << i)) != 0) {
          assignment[assignmentIndex + assignedCount] = i;
          nextMask |= (1 << i);
          if (wishes[i][slotIndex] == Wish.DESIRED) {
            comboScore++;
          }
          assignedCount++;
        }
      }

      int futureScore = computeMaxScore(wishes, slotIndex + 1, nextMask, memo);
      if (futureScore != IMPOSSIBLE && comboScore + futureScore == targetScore) {
        reconstructAssignment(
            wishes,
            slotIndex + 1,
            nextMask,
            futureScore,
            assignment,
            assignmentIndex + requiredCount,
            memo);
        return; // 最初の最大スコア案を採用（同点では更新しない）
      }
    }
  }

  /**
   * 割り当てから AssignmentResult を構築します。
   *
   * @param employees 有効な従業員リスト
   * @param assignment 従業員インデックスの割り当て（8 件の配列）
   * @return AssignmentResult
   */
  private AssignmentResult buildResult(List<Employee> employees, int[] assignment) {
    ShiftSlot[] slots = ShiftSlot.values();
    List<ShiftSlot> slotList = new ArrayList<>();

    for (int slotIndex = 0; slotIndex < slots.length; slotIndex++) {
      ShiftSlot slot = slots[slotIndex];
      int requiredCount = slot.numberOfEmployees();
      for (int i = 0; i < requiredCount; i++) {
        slotList.add(slot);
      }
    }

    BreakScheduler scheduler = new BreakScheduler();
    List<BreakInterval> breaks = scheduler.schedule(slotList);

    List<ShiftAssignment> shiftAssignments = new ArrayList<>();
    int score = 0;
    boolean[] used = new boolean[employees.size()];

    for (int i = 0; i < assignment.length; i++) {
      int employeeIndex = assignment[i];
      Employee employee = employees.get(employeeIndex);
      ShiftSlot slot = slotList.get(i);
      BreakInterval breakInterval = breaks.get(i);

      shiftAssignments.add(
          new ShiftAssignment(employee, slot, breakInterval.startTime(), breakInterval.endTime()));

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

    return new AssignmentResult(shiftAssignments, score, unassigned);
  }

  /**
   * ShiftSlot に対応するインデックスを返します（0-5）。
   *
   * @param slot 枠
   * @return インデックス
   */
  private int getSlotIndex(ShiftSlot slot) {
    ShiftSlot[] slots = ShiftSlot.values();
    for (int i = 0; i < slots.length; i++) {
      if (slots[i] == slot) {
        return i;
      }
    }
    throw new IllegalStateException("Slot index not found");
  }

  @Override
  public List<DuplicateNameError> findDuplicateNames(List<Employee> employees) {
    // V-2 の重複行番号を返すため、元のインデックスと氏名を対応させる
    List<Integer> validIndexes = new ArrayList<>();
    List<String> validNames = new ArrayList<>();
    for (int i = 0; i < employees.size(); i++) {
      String name = employees.get(i).name();
      if (name != null && !name.isBlank()) {
        validIndexes.add(i);
        validNames.add(name);
      }
    }

    List<DuplicateNameError> duplicates = new ArrayList<>();
    for (int i = 0; i < validNames.size(); i++) {
      String name = validNames.get(i);
      if (duplicates.stream().anyMatch(d -> d.name().equals(name))) {
        continue;
      }

      List<Integer> indices = new ArrayList<>();
      indices.add(validIndexes.get(i));
      for (int j = i + 1; j < validNames.size(); j++) {
        if (name.equals(validNames.get(j))) {
          indices.add(validIndexes.get(j));
        }
      }

      if (indices.size() > 1) {
        duplicates.add(new DuplicateNameError(name, indices));
      }
    }

    return duplicates;
  }
}
