package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalTime;
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

  @Nested
  @DisplayName("[F-1] 新仕様の従業員入力（時間帯・休み）")
  class EmployeeTimeRange {

    @Test
    @DisplayName(
        "[F-1] Given: Employee.working()で従業員を作成するとき, When: 属性にアクセスすると, Then:"
            + " nameと時間帯が取得でき、offがfalseである")
    void workingEmployeeHasCorrectAttributes() {
      String name = "山田太郎";
      LocalTime start = LocalTime.of(8, 0);
      LocalTime end = LocalTime.of(17, 0);

      Employee employee = Employee.working(name, start, end);

      assertEquals(name, employee.name());
      assertEquals(start, employee.start());
      assertEquals(end, employee.end());
      assertEquals(false, employee.off());
    }

    @Test
    @DisplayName(
        "[F-1] Given: Employee.onLeave()で従業員を作成するとき, When: 属性にアクセスすると, Then:"
            + " offがtrueで、startとendがnullである")
    void onLeaveEmployeeHasNullTimeRange() {
      String name = "山田太郎";

      Employee employee = Employee.onLeave(name);

      assertEquals(name, employee.name());
      assertEquals(true, employee.off());
      assertNull(employee.start());
      assertNull(employee.end());
    }
  }
}
