package com.example.shiftmatch.service;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.DuplicateNameError;
import com.example.shiftmatch.domain.Employee;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * シフト割り当てを行うサービスインターフェース。
 *
 * <p>従業員の勤務できる時間帯をもとに、ハード制約を満たし、スコア（ずれの合計）を最小化する割り当て案を算出します。
 */
public interface ShiftAssignmentService {

  /**
   * 従業員一覧をもとにシフト割り当てを行う。
   *
   * @param employees 従業員一覧
   * @return 割り当て結果。条件を満たす案がない場合は空の Optional を返す
   */
  Optional<AssignmentResult> assign(List<Employee> employees);

  /**
   * パートの週の残り実働時間を考慮して、シフト割り当てを行う。
   *
   * <p>週の上限（H-4）が適用される従業員は、実働時間（{@code ShiftSlot#netWorkMinutes()}）が残りを超える枠に入れない。
   * マップにない従業員と、上限が適用されない従業員は制限しない。
   *
   * @param employees 従業員一覧
   * @param remainingWeeklyMinutes 従業員名から、その週の残り実働分（分）への対応
   * @return 割り当て結果。条件を満たす案がない場合は空の Optional を返す
   */
  Optional<AssignmentResult> assign(
      List<Employee> employees, Map<String, Integer> remainingWeeklyMinutes);

  /**
   * 枠ごとに決まった担当者から、休憩時刻などを含む割り当て結果を組み立てる。
   *
   * <p>週全体の最適化（H-4）が担当者を決めた後に、1 日分の結果へ変換するために使う。
   *
   * @param employees その日の有効な従業員の一覧（入力順）
   * @param assigneesInSlotOrder 枠 1（2 名）→ 枠 6（2 名）の順に並べた 8 名。{@code employees} の要素と同一のインスタンス
   * @return 割り当て結果
   */
  AssignmentResult buildAssignment(List<Employee> employees, List<Employee> assigneesInSlotOrder);

  /**
   * 従業員一覧の中から重複する氏名を検出する。
   *
   * <p>氏名が{@code null}または{@code isBlank()}である従業員は除外してから重複判定を行う。
   * 重複がない場合は空リストを返す。同じ氏名が3件以上ある場合も1つの{@link DuplicateNameError}にまとめられる。
   *
   * @param employees 従業員一覧
   * @return 重複エラーのリスト。重複がない場合は空リスト
   */
  List<DuplicateNameError> findDuplicateNames(List<Employee> employees);
}
