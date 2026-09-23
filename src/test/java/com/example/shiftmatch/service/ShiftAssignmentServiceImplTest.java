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
  }
}
