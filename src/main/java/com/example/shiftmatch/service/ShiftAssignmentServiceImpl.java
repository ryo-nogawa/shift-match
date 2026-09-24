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
 * <p>6 種類の枠に 8 名を割り当てます。全組み合わせを総当たりで評価し、条件を満たす案の中で
 * スコアが最大かつ入力順で最初の案を返します。
 */
@Service
public class ShiftAssignmentServiceImpl implements ShiftAssignmentService {

  private static final int TOTAL_EMPLOYEES = 8;
  private static final int MIN_EMPLOYEES = 8;

  @Override
  public Optional<AssignmentResult> assign(List<Employee> employees) {
    // V-1: 氏名が空の行を処理対象から除外
    List<Employee> validEmployees =
        employees.stream().filter(emp -> emp.name() != null && !emp.name().isBlank()).toList();

    // V-4: 有効な従業員が8名未満
    if (validEmployees.size() < MIN_EMPLOYEES) {
      return Optional.empty();
    }

    // 希望を事前に展開（Wish[][]：従業員 × 枠）
    Wish[][] wishes = new Wish[validEmployees.size()][6];
    for (int i = 0; i < validEmployees.size(); i++) {
      List<Wish> employeeWishes = validEmployees.get(i).wishes();
      for (int j = 0; j < 6; j++) {
        wishes[i][j] = employeeWishes.get(j);
      }
    }

    // 最適な案を探す
    BestAssignment bestAssignment = new BestAssignment();
    boolean[] used = new boolean[validEmployees.size()];
    int[] assignment = new int[8];
    findBestAssignment(validEmployees, wishes, used, assignment, 0, 0, bestAssignment);

    if (bestAssignment.assignment == null) {
      return Optional.empty();
    }

    return Optional.of(buildResult(validEmployees, bestAssignment.assignment));
  }

  /**
   * 最適な割り当てを保持するクラス。
   */
  private static class BestAssignment {
    int[] assignment;
    int score;

    BestAssignment() {
      this.assignment = null;
      this.score = -1;
    }
  }

  /**
   * 再帰的に最適な割り当てを探します。
   *
   * @param employees 有効な従業員リスト
   * @param wishes 希望の2次元配列（従業員 × 枠）
   * @param used 割り当て済みの従業員フラグ
   * @param assignment 現在の割り当て（長さ8）
   * @param assignmentIndex 割り当て配列の次の位置
   * @param slotIndex 次に割り当てる枠のインデックス
   * @param bestAssignment 最適な割り当て
   */
  private void findBestAssignment(
      List<Employee> employees,
      Wish[][] wishes,
      boolean[] used,
      int[] assignment,
      int assignmentIndex,
      int slotIndex,
      BestAssignment bestAssignment) {

    if (slotIndex >= ShiftSlot.values().length) {
      // すべての枠を割り当てた：スコアを計算
      int score = calculateScore(employees, wishes, assignment);
      if (score > bestAssignment.score) {
        bestAssignment.score = score;
        bestAssignment.assignment = assignment.clone();
      }
      return;
    }

    ShiftSlot slot = ShiftSlot.values()[slotIndex];
    int requiredCount = slot.numberOfEmployees();

    // この枠に割り当てる従業員の組み合わせを探す
    findCombinationsOptimized(
        employees,
        wishes,
        used,
        assignment,
        assignmentIndex,
        0,
        requiredCount,
        slotIndex,
        bestAssignment);
  }

  /**
   * 与えられた枠に割り当てる従業員の組み合わせを列挙します（最適化版）。
   *
   * @param employees 有効な従業員リスト
   * @param wishes 希望の2次元配列
   * @param used 割り当て済みフラグ
   * @param assignment 現在の割り当て
   * @param assignmentIndex 割り当て配列の次の位置
   * @param candidateStart 次にチェックする候補の開始インデックス
   * @param requiredCount この枠に必要な人数
   * @param slotIndex 現在の枠のインデックス
   * @param bestAssignment 最適な割り当て
   */
  private void findCombinationsOptimized(
      List<Employee> employees,
      Wish[][] wishes,
      boolean[] used,
      int[] assignment,
      int assignmentIndex,
      int candidateStart,
      int requiredCount,
      int slotIndex,
      BestAssignment bestAssignment) {

    if (requiredCount == 0) {
      // この枠への割り当てが完成したら、次の枠へ
      findBestAssignment(
          employees, wishes, used, assignment, assignmentIndex, slotIndex + 1, bestAssignment);
      return;
    }

    for (int i = candidateStart; i < employees.size(); i++) {
      // この従業員が既に割り当てられていないか確認（H-2）
      if (used[i]) {
        continue;
      }

      // この従業員がこの枠で × でないか確認（H-3）
      if (wishes[i][slotIndex] == Wish.UNAVAILABLE) {
        continue;
      }

      // この従業員を割り当てる
      used[i] = true;
      assignment[assignmentIndex] = i;

      findCombinationsOptimized(
          employees,
          wishes,
          used,
          assignment,
          assignmentIndex + 1,
          i + 1,
          requiredCount - 1,
          slotIndex,
          bestAssignment);

      // バックトラック
      used[i] = false;
    }
  }

  /**
   * 割り当てのスコアを計算します。
   *
   * @param employees 有効な従業員リスト
   * @param wishes 希望の2次元配列
   * @param assignment 割り当て配列
   * @return スコア
   */
  private int calculateScore(List<Employee> employees, Wish[][] wishes, int[] assignment) {
    int score = 0;
    int position = 0;

    for (int slotIndex = 0; slotIndex < ShiftSlot.values().length; slotIndex++) {
      ShiftSlot slot = ShiftSlot.values()[slotIndex];
      int requiredCount = slot.numberOfEmployees();

      for (int i = 0; i < requiredCount; i++) {
        int employeeIndex = assignment[position];
        if (wishes[employeeIndex][slotIndex] == Wish.DESIRED) {
          score++;
        }
        position++;
      }
    }

    return score;
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

    // 割り当てから枠のリストを構築
    int position = 0;
    for (int slotIndex = 0; slotIndex < slots.length; slotIndex++) {
      ShiftSlot slot = slots[slotIndex];
      int requiredCount = slot.numberOfEmployees();
      for (int i = 0; i < requiredCount; i++) {
        slotList.add(slot);
        position++;
      }
    }

    // BreakScheduler で休憩時刻を割り当て
    BreakScheduler scheduler = new BreakScheduler();
    List<BreakInterval> breaks = scheduler.schedule(slotList);

    // ShiftAssignment のリストを構築
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

      // スコアを計算（◎の個数）
      int slotIndex = getSlotIndex(slot);
      if (employee.wishes().get(slotIndex) == Wish.DESIRED) {
        score++;
      }

      used[employeeIndex] = true;
    }

    // 未割り当て従業員を計算
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
    // V-2 で該当行を示すため、氏名と元のインデックスを同じ走査で対にして保持する
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
