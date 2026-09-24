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
  private static final int MAX_EMPLOYEES = 12;
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

    // V-5: 有効な従業員が13名以上
    if (validEmployees.size() > MAX_EMPLOYEES) {
      return Optional.empty();
    }

    List<AssignmentResult> solutions = new ArrayList<>();
    List<Integer> assignment = new ArrayList<>();
    findAssignments(validEmployees, assignment, 0, solutions);

    if (solutions.isEmpty()) {
      return Optional.empty();
    }

    // 最初に最大スコアに達した案を返す
    AssignmentResult best = solutions.get(0);
    for (AssignmentResult solution : solutions) {
      if (solution.score() > best.score()) {
        best = solution;
      }
    }

    return Optional.of(best);
  }

  /**
   * 枠 1 → 6 の順に従業員を割り当てます（再帰）。
   *
   * @param employees 有効な従業員リスト
   * @param assignment 現在の割り当て（従業員インデックスのリスト、8 件になったら完成）
   * @param slotIndex 次に割り当てる枠のインデックス（0 = 枠 1）
   * @param solutions 見つかった解のリスト
   */
  private void findAssignments(
      List<Employee> employees,
      List<Integer> assignment,
      int slotIndex,
      List<AssignmentResult> solutions) {
    if (slotIndex >= ShiftSlot.values().length) {
      // すべての枠を割り当てたら、解を構築
      AssignmentResult result = buildResult(employees, assignment);
      solutions.add(result);
      return;
    }

    ShiftSlot slot = ShiftSlot.values()[slotIndex];
    int requiredCount = slot.numberOfEmployees();

    // この枠に割り当てる従業員を探す（残りの従業員から）
    findCombinations(
        employees, assignment, 0, new ArrayList<>(), requiredCount, slotIndex, solutions);
  }

  /**
   * 与えられた枠に割り当てる従業員の組み合わせを列挙します。
   *
   * @param employees 有効な従業員リスト
   * @param assignment 現在の割り当て
   * @param candidateStart 次にチェックする候補の開始インデックス
   * @param currentSlotAssignment この枠に割り当てる従業員のインデックス
   * @param requiredCount この枠に必要な人数
   * @param slotIndex 現在の枠のインデックス
   * @param solutions 見つかった解のリスト
   */
  private void findCombinations(
      List<Employee> employees,
      List<Integer> assignment,
      int candidateStart,
      List<Integer> currentSlotAssignment,
      int requiredCount,
      int slotIndex,
      List<AssignmentResult> solutions) {

    if (currentSlotAssignment.size() == requiredCount) {
      // この枠への割り当てが完成したら、次の枠へ
      assignment.addAll(currentSlotAssignment);
      findAssignments(employees, assignment, slotIndex + 1, solutions);
      assignment.removeAll(currentSlotAssignment);
      return;
    }

    ShiftSlot slot = ShiftSlot.values()[slotIndex];

    for (int i = candidateStart; i < employees.size(); i++) {
      // この従業員が既に割り当てられていないか確認（H-2）
      if (assignment.contains(i)) {
        continue;
      }

      Employee employee = employees.get(i);

      // この従業員がこの枠で × でないか確認（H-3）
      if (employee.wishes().get(slotIndex) == Wish.UNAVAILABLE) {
        continue;
      }

      currentSlotAssignment.add(i);
      findCombinations(
          employees, assignment, i + 1, currentSlotAssignment, requiredCount, slotIndex, solutions);
      currentSlotAssignment.remove(currentSlotAssignment.size() - 1);
    }
  }

  /**
   * 割り当てから AssignmentResult を構築します。
   *
   * @param employees 有効な従業員リスト
   * @param assignment 従業員インデックスの割り当て（8 件）
   * @return AssignmentResult
   */
  private AssignmentResult buildResult(List<Employee> employees, List<Integer> assignment) {
    List<ShiftSlot> slots = new ArrayList<>();
    List<Employee> assignedEmployees = new ArrayList<>();

    for (int index : assignment) {
      Employee employee = employees.get(index);
      assignedEmployees.add(employee);

      // この従業員がどの枠に割り当てられたかを determine
      ShiftSlot slotForThisEmployee = getSlotForEmployee(assignment, index);
      slots.add(slotForThisEmployee);
    }

    // BreakScheduler で休憩時刻を割り当て
    BreakScheduler scheduler = new BreakScheduler();
    List<BreakInterval> breaks = scheduler.schedule(slots);

    // ShiftAssignment のリストを構築
    List<ShiftAssignment> shiftAssignments = new ArrayList<>();
    for (int i = 0; i < assignedEmployees.size(); i++) {
      shiftAssignments.add(
          new ShiftAssignment(
              assignedEmployees.get(i),
              slots.get(i),
              breaks.get(i).startTime(),
              breaks.get(i).endTime()));
    }

    // スコアを計算
    int score = 0;
    for (ShiftAssignment sa : shiftAssignments) {
      int slotIndex = getSlotIndex(sa.slot());
      if (sa.employee().wishes().get(slotIndex) == Wish.DESIRED) {
        score++;
      }
    }

    // 未割り当て従業員を計算
    List<Employee> unassigned = new ArrayList<>();
    for (int i = 0; i < employees.size(); i++) {
      if (!assignment.contains(i)) {
        unassigned.add(employees.get(i));
      }
    }

    return new AssignmentResult(shiftAssignments, score, unassigned);
  }

  /**
   * 従業員インデックスに対応する枠を返します。
   *
   * @param assignment 従業員インデックスの割り当て
   * @param employeeIndex 従業員のインデックス
   * @return 割り当てられた枠
   */
  private ShiftSlot getSlotForEmployee(List<Integer> assignment, int employeeIndex) {
    int positionInAssignment = assignment.indexOf(employeeIndex);
    ShiftSlot[] slots = ShiftSlot.values();

    int position = 0;
    for (int slotIndex = 0; slotIndex < slots.length; slotIndex++) {
      int requiredCount = slots[slotIndex].numberOfEmployees();
      if (positionInAssignment < position + requiredCount) {
        return slots[slotIndex];
      }
      position += requiredCount;
    }

    throw new IllegalStateException("Slot not found for employee");
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
