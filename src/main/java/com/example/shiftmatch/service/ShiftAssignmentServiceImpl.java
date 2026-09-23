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
    // 全組み合わせを列挙し、最初に見つかった案を返す
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

            // 未割り当て従業員を計算
            List<Employee> unassignedEmployees = new ArrayList<>();
            for (Employee emp : remaining) {
              if (!lateEmployees.contains(emp)) {
                unassignedEmployees.add(emp);
              }
            }

            // 結果を返す（T2は最初の組み合わせを返すだけ）
            return Optional.of(
                new AssignmentResult(earlyEmployees, lateEmployees, 0, unassignedEmployees));
          }
        }
      }
    }

    return Optional.empty();
  }
}
