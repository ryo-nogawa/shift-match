package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Employee")
class EmployeeTest {

  @Nested
  @DisplayName("[F-1] 従業員の希望")
  class EmployeeWishes {

    @Test
    @DisplayName("[F-1] Given: 6つの希望が与えられたとき, When: Employeeを生成すると, Then: 希望がそれぞれ取得できる")
    void canCreateEmployeeWithValidWishes() {
      String name = "山田太郎";
      List<Wish> wishes =
          List.of(
              Wish.DESIRED,
              Wish.AVAILABLE,
              Wish.UNAVAILABLE,
              Wish.DESIRED,
              Wish.AVAILABLE,
              Wish.UNAVAILABLE);

      Employee employee = new Employee(name, wishes);

      assertEquals(name, employee.name());
      assertEquals(wishes, employee.wishes());
    }

    @Test
    @DisplayName(
        "[F-1] Given: 希望の件数が6でないとき, When: Employeeを生成すると, Then: IllegalArgumentExceptionがスローされる")
    void throwsExceptionWhenWishesCountIsNotSix() {
      String name = "山田太郎";
      List<Wish> wishesWithWrongCount = List.of(Wish.DESIRED, Wish.AVAILABLE);

      assertThrows(IllegalArgumentException.class, () -> new Employee(name, wishesWithWrongCount));
    }

    @Test
    @DisplayName("[F-1] Given: 希望のリストが与えられたとき, When: wishesにアクセスすると, Then: 不変なリストが返される")
    void returnsImmutableWishes() {
      String name = "山田太郎";
      List<Wish> wishes =
          List.of(
              Wish.DESIRED,
              Wish.AVAILABLE,
              Wish.UNAVAILABLE,
              Wish.DESIRED,
              Wish.AVAILABLE,
              Wish.UNAVAILABLE);

      Employee employee = new Employee(name, wishes);
      List<Wish> returnedWishes = employee.wishes();

      assertThrows(UnsupportedOperationException.class, () -> returnedWishes.add(Wish.DESIRED));
    }
  }
}
