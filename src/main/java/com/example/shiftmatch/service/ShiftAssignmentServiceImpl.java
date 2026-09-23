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

    // 全組み合わせを列挙し、最大スコアの案を探す
    for (int i = 0; i < employees.size(); i++) {
      Employee empI = employees.get(i);
      // 早番×は除外
      if (empI.earlyWish() == Wish.UNAVAILABLE) {
        continue;
      }

      for (int j = i + 1; j < employees.size(); j++) {
        Employee empJ = employees.get(j);
        // 早番×は除外
        if (empJ.earlyWish() == Wish.UNAVAILABLE) {
          continue;
        }

        // 早番の組 (i, j)
        List<Employee> earlyEmployees = List.of(empI, empJ);

        // 残りの従業員を取得
        List<Employee> remaining = new ArrayList<>();
        for (int k = 0; k < employees.size(); k++) {
          if (k != i && k != j) {
            remaining.add(employees.get(k));
          }
        }

        // 残りの従業員から遅番2名を選ぶ
        for (int k = 0; k < remaining.size(); k++) {
          Employee empK = remaining.get(k);
          // 遅番×は除外
          if (empK.lateWish() == Wish.UNAVAILABLE) {
            continue;
          }

          for (int l = k + 1; l < remaining.size(); l++) {
            Employee empL = remaining.get(l);
            // 遅番×は除外
            if (empL.lateWish() == Wish.UNAVAILABLE) {
              continue;
            }

            // 遅番の組 (k, l)
            List<Employee> lateEmployees = List.of(empK, empL);

            // スコアを計算
            int score = calculateScore(earlyEmployees, lateEmployees);

            // 未割り当て従業員を計算
            List<Employee> unassignedEmployees = new ArrayList<>();
            for (Employee emp : remaining) {
              if (!lateEmployees.contains(emp)) {
                unassignedEmployees.add(emp);
              }
            }

            // スコアが最大値より大きい場合に更新（同点では更新しない）
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
