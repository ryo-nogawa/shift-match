package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

  @Nested
  class 重複ありの場合 {

    @Test
    @DisplayName(
        "[V-2] Given: 有効な氏名のうち1組（2件）が重複しているとき, When: findDuplicateNamesを実行すると,"
            + " Then: DuplicateNameErrorが1件返り、rowIndexesが該当する元のインデックスと一致する")
    void returnsDuplicateErrorWhenTwoNamesAreDuplicated() {
      // Given: 太郎が0と2のインデックスで重複
      List<Employee> employees =
          List.of(
              new Employee("太郎", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("花子", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("太郎", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("美咲", Wish.AVAILABLE, Wish.AVAILABLE));
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      // When
      List<DuplicateNameError> result = service.findDuplicateNames(employees);

      // Then
      assertEquals(1, result.size());
      DuplicateNameError error = result.get(0);
      assertEquals("太郎", error.name());
      assertEquals(List.of(0, 2), error.rowIndexes());
    }

    @Test
    @DisplayName(
        "[V-2] Given: 同じ氏名が3件以上重複しているとき, When: findDuplicateNamesを実行すると,"
            + " Then: 1件のDuplicateNameErrorにまとまり、rowIndexesに該当する全インデックスが含まれる")
    void returnsSingleErrorWhenMoreThanTwoDuplicates() {
      // Given: 太郎が0, 2, 4のインデックスで重複
      List<Employee> employees =
          List.of(
              new Employee("太郎", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("花子", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("太郎", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("美咲", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("太郎", Wish.AVAILABLE, Wish.AVAILABLE));
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      // When
      List<DuplicateNameError> result = service.findDuplicateNames(employees);

      // Then
      assertEquals(1, result.size());
      DuplicateNameError error = result.get(0);
      assertEquals("太郎", error.name());
      assertEquals(List.of(0, 2, 4), error.rowIndexes());
    }

    @Test
    @DisplayName(
        "[V-2] Given: 氏名が空の行を含みつつ、有効な行同士で氏名が重複しているとき, When:"
            + " findDuplicateNamesを実行すると, Then: 空行はカウントされず、有効な行のインデックスのみがrowIndexesに含まれる")
    void excludesBlankNamesWhenDetectingDuplicates() {
      // Given: インデックス1と3に"太郎"があり、2に空の行がある
      List<Employee> employees =
          List.of(
              new Employee("花子", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("太郎", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("太郎", Wish.AVAILABLE, Wish.AVAILABLE),
              new Employee("美咲", Wish.AVAILABLE, Wish.AVAILABLE));
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      // When
      List<DuplicateNameError> result = service.findDuplicateNames(employees);

      // Then
      assertEquals(1, result.size());
      DuplicateNameError error = result.get(0);
      assertEquals("太郎", error.name());
      // 元のリストにおけるインデックス（1と3）
      assertEquals(List.of(1, 3), error.rowIndexes());
    }
  }
}
