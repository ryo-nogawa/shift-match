package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.shiftmatch.domain.DuplicateNameError;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.Wish;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ShiftAssignmentServiceImplFindDuplicateNamesTest {

  @Nested
  class 正常系 {

    @Test
    @DisplayName("[V-2] Given: 有効な氏名がすべて異なるとき, When: findDuplicateNamesを実行すると," + " Then: 空リストが返る")
    void returnsEmptyListWhenNoNamesAreDuplicated() {
      // Given
      List<Employee> employees =
          List.of(
              new Employee("太郎", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("花子", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("次郎", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("美咲", Wish.AVAILABLE, Wish.AVAILABLE));
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      // When
      List<DuplicateNameError> result = service.findDuplicateNames(employees);

      // Then
      assertTrue(result.isEmpty());
    }
  }
}
