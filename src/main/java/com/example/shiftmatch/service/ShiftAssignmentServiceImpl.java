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

    // 新仕様：off == false（休みでない）かつ、開始・終了が設定されている従業員を候補とする
    List<Employee> candidates =
        validEmployees.stream()
            .filter(emp -> !emp.off() && emp.start() != null && emp.end() != null)
            .toList();

    // V-4：候補が 8 名未満なら不成立
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

    int maxScore = computeMaxScore(candidates, 0, 0, memo);

    if (maxScore == IMPOSSIBLE) {
      return Optional.empty();
    }

    // 復元：入力順の辞書順で最初の最大スコア案を構築
    int[] assignment = new int[ShiftSlot.totalEmployees()];
    reconstructAssignment(candidates, 0, 0, maxScore, assignment, 0, memo);

    return Optional.of(buildResult(validEmployees, candidates, assignment));
  }

  /**
   * 動的計画法で最大スコアを計算します。
   *
   * @param candidates 候補となる従業員リスト（新仕様）
   * @param slotIndex 現在の枠インデックス
   * @param usedMask 使用済み従業員のビットマスク
   * @param memo メモ化テーブル
   * @return 枠 slotIndex 以降で得られる最大の追加スコア（割り当て不可なら {@code IMPOSSIBLE}）
   */
  private int computeMaxScore(
      List<Employee> candidates, int slotIndex, int usedMask, int[][] memo) {
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
    for (int combo : generateCombinations(candidates, slotIndex, usedMask, requiredCount)) {
      int nextMask = usedMask;
      int comboScore = 0;

      for (int i = 0, bit = 0; i < candidates.size() && i < 32; i++) {
        if ((combo & (1 << i)) != 0) {
          nextMask |= (1 << i);
          // 新仕様：◎の数ではなく、スコアは後で計算（T6で変更予定）
          comboScore += 0;
          bit++;
          if (bit >= requiredCount) {
            break;
          }
        }
      }

      int futureScore = computeMaxScore(candidates, slotIndex + 1, nextMask, memo);
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
   * @param candidates 候補となる従業員リスト（新仕様）
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
   * 復元：入力順の辞書順で最初の最大スコア案を構築します。
   *
   * <p>仕様 5.3 節の「列挙順で最初に最大スコアへ到達する案」と同じ案を得るため、 入力順の辞書順で組を列挙し、スコア条件を満たす最初の組を選びます。
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
          assignment[assignmentIndex + assignedCount] = i;
          nextMask |= (1 << i);
          // 新仕様：スコアは後で計算（T6で変更予定）
          comboScore += 0;
          assignedCount++;
        }
      }

      int futureScore = computeMaxScore(candidates, slotIndex + 1, nextMask, memo);
      if (futureScore != IMPOSSIBLE && comboScore + futureScore == targetScore) {
        reconstructAssignment(
            candidates,
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

      // 新仕様：スコアは T6 で計算予定。当面は 0。
      score += 0;

      used[employeeIndex] = true;
    }

    // 未出勤者：割り当てられなかった有効な従業員と「休み」の従業員の両方を入力順で返す
    List<Employee> unassigned = new ArrayList<>();
    for (int i = 0; i < allEmployees.size(); i++) {
      Employee emp = allEmployees.get(i);

      // 氏名が空でない（有効）かどうか確認
      if (emp.name() == null || emp.name().isBlank()) {
        continue;
      }

      // 「休み」の従業員は未出勤者に含める
      if (emp.off()) {
        unassigned.add(emp);
        continue;
      }

      // 有効で、休みでない従業員：候補リストから割り当てられたか確認
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
