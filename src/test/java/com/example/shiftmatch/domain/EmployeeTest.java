package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EmployeeTest {

  @Nested
  class 正常系 {

    @Test
    @DisplayName("Given: 有効な従業員情報が与えられたとき, When: Employeeレコードを生成すると, Then: nameがそれぞれ取得できる")
    void canCreateEmployeeWithValidName() {
      // Given
      String expectedName = "山田太郎";

      // When
      Employee employee = new Employee(expectedName, Wish.DESIRED, Wish.AVAILABLE);

      // Then
      assertEquals(expectedName, employee.name());
    }

    @Test
    @DisplayName("Given: 有効な従業員情報が与えられたとき, When: Employeeレコードを生成すると, Then: earlyWishが取得できる")
    void canCreateEmployeeWithValidEarlyWish() {
      // Given
      Wish expectedEarlyWish = Wish.DESIRED;

      // When
      Employee employee = new Employee("山田太郎", expectedEarlyWish, Wish.AVAILABLE);

      // Then
      assertEquals(expectedEarlyWish, employee.earlyWish());
    }

    @Test
    @DisplayName("Given: 有効な従業員情報が与えられたとき, When: Employeeレコードを生成すると, Then: lateWishが取得できる")
    void canCreateEmployeeWithValidLateWish() {
      // Given
      Wish expectedLateWish = Wish.UNAVAILABLE;

      // When
      Employee employee = new Employee("山田太郎", Wish.DESIRED, expectedLateWish);

      // Then
      assertEquals(expectedLateWish, employee.lateWish());
    }
  }
}
