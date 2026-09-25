package com.example.shiftmatch.service;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.BreakInterval;
import com.example.shiftmatch.domain.BreakScheduler;
import com.example.shiftmatch.domain.DuplicateNameError;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.ShiftAssignment;
import com.example.shiftmatch.domain.ShiftSlot;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * シフト割り当てを行うサービス実装。
 *
 * <p>6 種類の枠に 8 名を割り当てます。動的計画法で最適な案を高速に求め、条件を満たす案の
 * 中でずれの合計が最小かつ入力順で最初の案を返します。
 */
@Service
public class ShiftAssignmentServiceImpl implements ShiftAssignmentService {

  private static final int UNCOMPUTED = -2;
  private static final int IMPOSSIBLE = -1;

  @Override
  public Optional<AssignmentResult> assign(List<Employee> employees) {
    List<Employee> validEmployees =
        employees.stream().filter(emp -> emp.name() != null && !emp.name().isBlank()).toList();

    // V-4 は「休みでない人」の人数で不成立を判定するため、休みの人を除いてから数える。
    // 開始・終了のない人は H-3 によりどの枠にも入れないので、候補にも含めない
    List<Employee> candidates =
        validEmployees.stream()
            .filter(emp -> !emp.off() && emp.start() != null && emp.end() != null)
            .toList();

    if (candidates.size() < ShiftSlot.totalEmployees()) {
      return Optional.empty();
    }

    int n = candidates.size();
    int[][] memo = new int[ShiftSlot.values().length + 1][1 << n];
    for (int i = 0; i < ShiftSlot.values().length + 1; i++) {
      for (int j = 0; j < (1 << n); j++) {
        memo[i][j] = UNCOMPUTED;
      }
    }

    int minScore = computeMinScore(candidates, 0, 0, memo);

    if (minScore == IMPOSSIBLE) {
      return Optional.empty();
    }

    // F-3（5.3 節）の同点規則に従い、列挙順で最初に最小スコアへ到達する案を返すため、
    // 最小スコアを求めた後に辞書順で案を復元する
    int[] assignment = new int[ShiftSlot.totalEmployees()];
    reconstructAssignment(candidates, 0, 0, minScore, assignment, 0, memo);

    return Optional.of(buildResult(validEmployees, candidates, assignment));
  }

  /**
   * 動的計画法で最小スコアを計算します。
   *
   * @param candidates 候補となる従業員リスト
   * @param slotIndex 現在の枠インデックス
   * @param usedMask 使用済み従業員のビットマスク
   * @param memo メモ化テーブル
   * @return 枠 slotIndex 以降で得られる最小の追加スコア（割り当て不可なら {@code IMPOSSIBLE}）
   */
  private int computeMinScore(
      List<Employee> candidates, int slotIndex, int usedMask, int[][] memo) {
    if (slotIndex >= ShiftSlot.values().length) {
      return 0;
    }

    if (memo[slotIndex][usedMask] != UNCOMPUTED) {
      return memo[slotIndex][usedMask];
    }

    ShiftSlot slot = ShiftSlot.values()[slotIndex];
    int requiredCount = slot.numberOfEmployees();
    int minScore = IMPOSSIBLE;

    for (int combo : generateCombinations(candidates, slotIndex, usedMask, requiredCount)) {
      int nextMask = usedMask;
      int comboScore = 0;

      for (int i = 0, bit = 0; i < candidates.size() && i < 32; i++) {
        if ((combo & (1 << i)) != 0) {
          Employee emp = candidates.get(i);
          comboScore += emp.gapMinutes(slot);
          nextMask |= (1 << i);
          bit++;
          if (bit >= requiredCount) {
            break;
          }
        }
      }

      int futureScore = computeMinScore(candidates, slotIndex + 1, nextMask, memo);
      if (futureScore != IMPOSSIBLE) {
        int totalScore = comboScore + futureScore;
        // 同点で更新すると F-3 の同点規則（列挙順で最初の案）に反するため、厳密に小さいときだけ更新する
        if (minScore == IMPOSSIBLE || totalScore < minScore) {
          minScore = totalScore;
        }
      }
    }

    memo[slotIndex][usedMask] = minScore;
    return minScore;
  }

  /**
   * 指定された枠に割り当て可能な従業員の組み合わせを列挙します。
   *
   * @param candidates 候補となる従業員リスト
   * @param slotIndex 枠インデックス
   * @param usedMask 使用済み従業員のビットマスク
   * @param requiredCount この枠に必要な人数
   * @return 組み合わせのリスト（各要素は従業員インデックスを示すビットマスク）
   */
  private java.util.List<Integer> generateCombinations(
      List<Employee> candidates, int slotIndex, int usedMask, int requiredCount) {
    java.util.List<Integer> combinations = new ArrayList<>();
    ShiftSlot slot = ShiftSlot.values()[slotIndex];
    combinationHelper(candidates, slot, usedMask, requiredCount, 0, 0, 0, combinations);
    return combinations;
  }

  /**
   * 組み合わせを再帰的に生成します。
   */
  private void combinationHelper(
      List<Employee> candidates,
      ShiftSlot slot,
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

    for (int i = currentIndex; i < candidates.size(); i++) {
      if ((usedMask & (1 << i)) == 0 && candidates.get(i).canWork(slot)) {
        combinationHelper(
            candidates,
            slot,
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
   * 入力順の辞書順で最初の最小スコア案を復元します。
   *
   * <p>F-3 の同点規則（5.3 節の「列挙順で最初に最小スコアへ到達する案」）と同じ案を得るため、入力順の辞書順で組を列挙し、スコア条件を満たす最初の組を選びます。
   */
  private void reconstructAssignment(
      List<Employee> candidates,
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

    for (int combo : generateCombinations(candidates, slotIndex, usedMask, requiredCount)) {
      int nextMask = usedMask;
      int comboScore = 0;

      int assignedCount = 0;
      for (int i = 0; i < candidates.size() && assignedCount < requiredCount; i++) {
        if ((combo & (1 << i)) != 0) {
          Employee emp = candidates.get(i);
          assignment[assignmentIndex + assignedCount] = i;
          nextMask |= (1 << i);
          comboScore += emp.gapMinutes(slot);
          assignedCount++;
        }
      }

      int futureScore = computeMinScore(candidates, slotIndex + 1, nextMask, memo);
      // 条件を満たす最初の組で確定し、同点の後続の組には切り替えない（F-3）
      if (futureScore != IMPOSSIBLE && comboScore + futureScore == targetScore) {
        reconstructAssignment(
            candidates,
            slotIndex + 1,
            nextMask,
            futureScore,
            assignment,
            assignmentIndex + requiredCount,
            memo);
        return;
      }
    }
  }

  /**
   * 割り当てから AssignmentResult を構築します。
   *
   * @param allEmployees すべての従業員リスト（有効な従業員と無効な従業員を含む）
   * @param candidates 割り当て対象の候補従業員リスト
   * @param assignment 従業員インデックスの割り当て（8 件の配列）
   * @return AssignmentResult
   */
  private AssignmentResult buildResult(
      List<Employee> allEmployees, List<Employee> candidates, int[] assignment) {
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
    boolean[] used = new boolean[candidates.size()];

    for (int i = 0; i < assignment.length; i++) {
      int employeeIndex = assignment[i];
      Employee employee = candidates.get(employeeIndex);
      ShiftSlot slot = slotList.get(i);
      BreakInterval breakInterval = breaks.get(i);

      shiftAssignments.add(
          new ShiftAssignment(employee, slot, breakInterval.startTime(), breakInterval.endTime()));

      score += employee.gapMinutes(slot);

      used[employeeIndex] = true;
    }

    // 7 章の出力仕様で、未出勤者は割り当てられなかった人と「休み」の人の両方を入力順で示すため、
    // 候補（休みを除いた人）ではなく有効な従業員全体から集める
    List<Employee> unassigned = new ArrayList<>();
    for (int i = 0; i < allEmployees.size(); i++) {
      Employee emp = allEmployees.get(i);

      if (emp.name() == null || emp.name().isBlank()) {
        continue;
      }

      if (emp.off()) {
        unassigned.add(emp);
        continue;
      }

      boolean assigned = false;
      for (int j = 0; j < candidates.size(); j++) {
        if (candidates.get(j) == emp && used[j]) {
          assigned = true;
          break;
        }
      }
      if (!assigned) {
        unassigned.add(emp);
      }
    }

    return new AssignmentResult(shiftAssignments, score, unassigned);
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
