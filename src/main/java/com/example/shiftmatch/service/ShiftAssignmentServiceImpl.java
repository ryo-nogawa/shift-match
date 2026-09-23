package com.example.shiftmatch.service;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.Wish;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * シフト割り当てを行うサービス実装。
 *
 * <p>全組み合わせを総当たりで評価し、条件を満たす案の中でスコアが最大かつ入力順で最初の案を返します。
 */
public class ShiftAssignmentServiceImpl implements ShiftAssignmentService {

  @Override
  public Optional<AssignmentResult> assign(List<Employee> employees) {
    AssignmentResult bestResult = null;
    int bestScore = -1;

    for (int i = 0; i < employees.size(); i++) {
      Employee empI = employees.get(i);
      if (empI.earlyWish() == Wish.UNAVAILABLE) {
        continue;
      }

      for (int j = i + 1; j < employees.size(); j++) {
        Employee empJ = employees.get(j);
        if (empJ.earlyWish() == Wish.UNAVAILABLE) {
          continue;
        }

        List<Employee> earlyEmployees = List.of(empI, empJ);

        // H-3（1人1枠まで）を満たすため、早番に選んだ2名を遅番の候補から除外する
        List<Employee> remaining = new ArrayList<>();
        for (int k = 0; k < employees.size(); k++) {
          if (k != i && k != j) {
            remaining.add(employees.get(k));
          }
        }

        for (int k = 0; k < remaining.size(); k++) {
          Employee empK = remaining.get(k);
          if (empK.lateWish() == Wish.UNAVAILABLE) {
            continue;
          }

          for (int l = k + 1; l < remaining.size(); l++) {
            Employee empL = remaining.get(l);
            if (empL.lateWish() == Wish.UNAVAILABLE) {
              continue;
            }

            List<Employee> lateEmployees = List.of(empK, empL);
            int score = calculateScore(earlyEmployees, lateEmployees);

            List<Employee> unassignedEmployees = new ArrayList<>();
            for (Employee emp : remaining) {
              if (!lateEmployees.contains(emp)) {
                unassignedEmployees.add(emp);
              }
            }

            // 仕様5.3節：スコアが同点の場合は列挙順で最初に到達した案を採用するため、
            // `>` で比較し同点では更新しない
            if (score > bestScore) {
              bestScore = score;
              bestResult =
                  new AssignmentResult(earlyEmployees, lateEmployees, score, unassignedEmployees);
            }
          }
        }
      }
    }

    return bestResult != null ? Optional.of(bestResult) : Optional.empty();
  }

  /**
   * 割り当てのスコアを計算する。
   *
   * <p>スコア = 割り当てられた4名のうち、その枠を◎と申告していた人数（0〜4）
   *
   * @param earlyEmployees 早番に割り当てられた従業員
   * @param lateEmployees 遅番に割り当てられた従業員
   * @return スコア（0〜4）
   */
  private int calculateScore(List<Employee> earlyEmployees, List<Employee> lateEmployees) {
    int score = 0;
    for (Employee emp : earlyEmployees) {
      if (emp.earlyWish() == Wish.DESIRED) {
        score++;
      }
    }
    for (Employee emp : lateEmployees) {
      if (emp.lateWish() == Wish.DESIRED) {
        score++;
      }
    }
    return score;
  }
}
