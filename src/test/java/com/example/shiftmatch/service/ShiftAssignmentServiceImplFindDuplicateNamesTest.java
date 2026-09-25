package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.shiftmatch.domain.DuplicateNameError;
import com.example.shiftmatch.domain.Employee;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ShiftAssignmentServiceImpl.findDuplicateNames")
class ShiftAssignmentServiceImplFindDuplicateNamesTest {

  @Nested
  class 正常系 {

    @Test
    @DisplayName("[V-2] Given: 有効な氏名がすべて異なるとき, When: findDuplicateNamesを実行すると, Then: 空リストが返る")
    void returnsEmptyListWhenNoNamesAreDuplicated() {
      List<Employee> employees =
          List.of(
              createEmployee("太郎"),
              createEmployee("花子"),
              createEmployee("次郎"),
              createEmployee("美咲"));
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      List<DuplicateNameError> result = service.findDuplicateNames(employees);

      assertTrue(result.isEmpty());
    }
  }

  @Nested
  class 重複ありの場合 {

    @Test
    @DisplayName(
        "[V-2] Given: 有効な氏名のうち1組（2件）が重複しているとき, When: findDuplicateNamesを実行すると, Then:"
            + " DuplicateNameErrorが1件返り、rowIndexesが該当する元のインデックスと一致する")
    void returnsDuplicateErrorWhenTwoNamesAreDuplicated() {
      List<Employee> employees =
          List.of(
              createEmployee("太郎"),
              createEmployee("花子"),
              createEmployee("太郎"),
              createEmployee("美咲"));
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      List<DuplicateNameError> result = service.findDuplicateNames(employees);

      assertEquals(1, result.size());
      DuplicateNameError error = result.get(0);
      assertEquals("太郎", error.name());
      assertEquals(List.of(0, 2), error.rowIndexes());
    }

    @Test
    @DisplayName(
        "[V-2] Given: 同じ氏名が3件以上重複しているとき, When: findDuplicateNamesを実行すると, Then:"
            + " 1件のDuplicateNameErrorにまとまり、rowIndexesに該当する全インデックスが含まれる")
    void returnsSingleErrorWhenMoreThanTwoDuplicates() {
      List<Employee> employees =
          List.of(
              createEmployee("太郎"),
              createEmployee("花子"),
              createEmployee("太郎"),
              createEmployee("美咲"),
              createEmployee("太郎"));
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      List<DuplicateNameError> result = service.findDuplicateNames(employees);

      assertEquals(1, result.size());
      DuplicateNameError error = result.get(0);
      assertEquals("太郎", error.name());
      assertEquals(List.of(0, 2, 4), error.rowIndexes());
    }

    @Test
    @DisplayName(
        "[V-2] Given: 氏名が空の行を含みつつ、有効な行同士で氏名が重複しているとき, When: findDuplicateNamesを実行すると, Then:"
            + " 空行はカウントされず、有効な行のインデックスのみがrowIndexesに含まれる")
    void excludesBlankNamesWhenDetectingDuplicates() {
      List<Employee> employees =
          List.of(
              createEmployee("花子"),
              createEmployee("太郎"),
              createEmployee(""),
              createEmployee("太郎"),
              createEmployee("美咲"));
      ShiftAssignmentService service = new ShiftAssignmentServiceImpl();

      List<DuplicateNameError> result = service.findDuplicateNames(employees);

      assertEquals(1, result.size());
      DuplicateNameError error = result.get(0);
      assertEquals("太郎", error.name());
      assertEquals(List.of(1, 3), error.rowIndexes());
    }
  }

  private Employee createEmployee(String name) {
    return Employee.working(name, LocalTime.of(7, 30), LocalTime.of(18, 30));
  }
}
