package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.Wish;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ShiftAssignmentServiceImplTest {

  @Nested
  class 正常系 {

    @Test
    @DisplayName(
        "[F-3] Given: 有効な従業員がちょうど4名で全員が早番・遅番ともにAVAILABLEのとき, When: assignを実行すると, Then:"
            + " 入力順インデックスの辞書順で最初の組み合わせが返る")
    void returnsFirstLexicographicCombinationWhenFourEmployeesAllAvailable() {
      // Given
      List<Employee> employees =
          List.of(
              new Employee("太郎", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("花子", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("次郎", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("美咲", Wish.AVAILABLE, Wish.AVAILABLE));
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      // When
      Optional<AssignmentResult> result = service.assign(employees);

      // Then
      assertTrue(result.isPresent());
      AssignmentResult assignment = result.get();

      // 早番は入力順の先頭2名（太郎、花子）
      assertEquals(2, assignment.earlyEmployees().size());
      assertEquals("太郎", assignment.earlyEmployees().get(0).name());
      assertEquals("花子", assignment.earlyEmployees().get(1).name());

      // 遅番は残り2名（次郎、美咲）
      assertEquals(2, assignment.lateEmployees().size());
      assertEquals("次郎", assignment.lateEmployees().get(0).name());
      assertEquals("美咲", assignment.lateEmployees().get(1).name());
    }

    @Test
    @DisplayName(
        "[H-3] Given: 5名の従業員全員が早番・遅番ともにAVAILABLEのとき, When: assignを実行すると, Then: 早番と遅番に同一の従業員が重複しない")
    void noEmployeeDuplicationBetweenEarlyAndLate() {
      // Given
      List<Employee> employees =
          List.of(
              new Employee("太郎", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("花子", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("次郎", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("美咲", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("健太", Wish.AVAILABLE, Wish.AVAILABLE));
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      // When
      Optional<AssignmentResult> result = service.assign(employees);

      // Then
      assertTrue(result.isPresent());
      AssignmentResult assignment = result.get();

      // 早番と遅番に重複がないことを確認
      for (Employee early : assignment.earlyEmployees()) {
        for (Employee late : assignment.lateEmployees()) {
          assertTrue(!early.equals(late), "同一の従業員が早番と遅番の両方に割り当てられています");
        }
      }
    }

    @Test
    @DisplayName("[H-4] Given: 早番希望が×の従業員を含む5名がいるとき, When: assignを実行すると, Then: その従業員が早番の案に含まれない")
    void excludesUnavailableEmployeeFromEarlyShift() {
      // Given: 花子の早番が×
      List<Employee> employees =
          List.of(
              new Employee("太郎", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("花子", Wish.UNAVAILABLE, Wish.AVAILABLE),
              new Employee("次郎", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("美咲", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("健太", Wish.AVAILABLE, Wish.AVAILABLE));
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      // When
      Optional<AssignmentResult> result = service.assign(employees);

      // Then
      assertTrue(result.isPresent());
      AssignmentResult assignment = result.get();

      // 花子が早番に含まれていないことを確認
      boolean hasHanako = assignment.earlyEmployees().stream().anyMatch(e -> "花子".equals(e.name()));
      assertTrue(!hasHanako, "早番×の花子が早番に割り当てられています");
    }

    @Test
    @DisplayName("[H-4] Given: 遅番希望が×の従業員を含む5名がいるとき, When: assignを実行すると, Then: その従業員が遅番の案に含まれない")
    void excludesUnavailableEmployeeFromLateShift() {
      // Given: 次郎の遅番が×
      List<Employee> employees =
          List.of(
              new Employee("太郎", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("花子", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("次郎", Wish.AVAILABLE, Wish.UNAVAILABLE),
              new Employee("美咲", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("健太", Wish.AVAILABLE, Wish.AVAILABLE));
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      // When
      Optional<AssignmentResult> result = service.assign(employees);

      // Then
      assertTrue(result.isPresent());
      AssignmentResult assignment = result.get();

      // 次郎が遅番に含まれていないことを確認
      boolean hasJiro = assignment.lateEmployees().stream().anyMatch(e -> "次郎".equals(e.name()));
      assertTrue(!hasJiro, "遅番×の次郎が遅番に割り当てられています");
    }

    @Test
    @DisplayName("[F-3] Given: スコア計算できる従業員構成のとき, When: assignを実行すると, Then: スコアが期待値と一致する")
    void calculatesScoreCorrectly() {
      // Given: 太郎・花子が◎、次郎・美咲が○
      List<Employee> employees =
          List.of(
              new Employee("太郎", Wish.DESIRED, Wish.DESIRED),
              new Employee("花子", Wish.DESIRED, Wish.AVAILABLE),
              new Employee("次郎", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("美咲", Wish.AVAILABLE, Wish.AVAILABLE));
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      // When
      Optional<AssignmentResult> result = service.assign(employees);

      // Then
      assertTrue(result.isPresent());
      AssignmentResult assignment = result.get();
      // スコア: 太郎（早番◎）+ 花子（遅番◎）= 2
      assertEquals(2, assignment.score());
    }

    @Test
    @DisplayName(
        "[F-3] Given: 複数の組み合わせが存在し、スコアが異なるとき, When: assignを実行すると, Then: スコアが最大の組み合わせが採用される")
    void adoptsMaximumScoreCombination() {
      // Given: 5名で、特定の組み合わせだけスコアが高くなるよう設計
      // 太郎と花子が早番と遅番で◎、他は○のみ
      List<Employee> employees =
          List.of(
              new Employee("太郎", Wish.DESIRED, Wish.AVAILABLE),
              new Employee("花子", Wish.AVAILABLE, Wish.DESIRED),
              new Employee("次郎", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("美咲", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("健太", Wish.AVAILABLE, Wish.AVAILABLE));
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      // When
      Optional<AssignmentResult> result = service.assign(employees);

      // Then
      assertTrue(result.isPresent());
      AssignmentResult assignment = result.get();
      // スコアが2以上（太郎の早番◎と花子の遅番◎）であることを確認
      assertTrue(assignment.score() >= 2, "スコアが最大化されていません: " + assignment.score());
    }

    @Test
    @DisplayName(
        "[F-3] Given: スコアが同点になる複数の組み合わせが存在するとき, When: assignを実行すると, Then:"
            + " 入力順インデックスの辞書順で最初の組み合わせが採用される")
    void selectsFirstLexicographicCombinationOnTie() {
      // Given: 4名全員◎なので、すべての組み合わせがスコア4で同点
      List<Employee> employees =
          List.of(
              new Employee("太郎", Wish.DESIRED, Wish.DESIRED),
              new Employee("花子", Wish.DESIRED, Wish.DESIRED),
              new Employee("次郎", Wish.DESIRED, Wish.DESIRED),
              new Employee("美咲", Wish.DESIRED, Wish.DESIRED));
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      // When
      Optional<AssignmentResult> result = service.assign(employees);

      // Then
      assertTrue(result.isPresent());
      AssignmentResult assignment = result.get();
      // スコアはすべて4で同点
      assertEquals(4, assignment.score());
      // 最初の組み合わせ：早番(0,1)、遅番(2,3)が採用される
      assertEquals("太郎", assignment.earlyEmployees().get(0).name());
      assertEquals("花子", assignment.earlyEmployees().get(1).name());
      assertEquals("次郎", assignment.lateEmployees().get(0).name());
      assertEquals("美咲", assignment.lateEmployees().get(1).name());
    }
  }

  @Nested
  class 不成立 {

    @Test
    @DisplayName(
        "[F-3] Given: 早番希望・遅番希望のいずれかがAVAILABLE以上な従業員が4名未満のとき, When: assignを実行すると, Then:"
            + " Optionalが空になる")
    void returnsEmptyWhenNotEnoughValidEmployees() {
      // Given: 3名のみ有効（花子は両方UNAVAILABLE）
      List<Employee> employees =
          List.of(
              new Employee("太郎", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("花子", Wish.UNAVAILABLE, Wish.UNAVAILABLE),
              new Employee("次郎", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("美咲", Wish.AVAILABLE, Wish.AVAILABLE));
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      // When
      Optional<AssignmentResult> result = service.assign(employees);

      // Then
      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName(
        "[F-3] Given: 早番可2名と遅番可2名が同一人物のため、組み合わせが作れないとき, When: assignを実行すると, Then: Optionalが空になる")
    void returnsEmptyWhenNoValidCombinationDueToDependency() {
      // Given: 太郎と花子しか早番可・遅番可だが、両方に割り当てられないため成立不可
      List<Employee> employees =
          List.of(
              new Employee("太郎", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("花子", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("次郎", Wish.UNAVAILABLE, Wish.UNAVAILABLE),
              new Employee("美咲", Wish.UNAVAILABLE, Wish.UNAVAILABLE));
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      // When
      Optional<AssignmentResult> result = service.assign(employees);

      // Then
      assertTrue(result.isEmpty());
    }
  }
}
