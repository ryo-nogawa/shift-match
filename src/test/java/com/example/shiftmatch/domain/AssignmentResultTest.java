package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AssignmentResultTest {

  @Nested
  class 正常系 {

    @Test
    @DisplayName(
        "[T-1] Given: 早番2名・遅番2名の割り当て結果が与えられたとき, When: breakTimes()を実行すると, Then:"
            + " 先頭2件が早番1人目=13:00~14:00、早番2人目=14:00~15:00である")
    void earlyEmployeesBreakTimesAreAssignedCorrectly() {
      // Given
      Employee earlyEmployee1 = new Employee("山田太郎", Wish.DESIRED, Wish.AVAILABLE);
      Employee earlyEmployee2 = new Employee("鈴木花子", Wish.AVAILABLE, Wish.DESIRED);
      Employee lateEmployee1 = new Employee("佐藤次郎", Wish.DESIRED, Wish.AVAILABLE);
      Employee lateEmployee2 = new Employee("田中美咲", Wish.AVAILABLE, Wish.DESIRED);

      AssignmentResult result =
          new AssignmentResult(
              List.of(earlyEmployee1, earlyEmployee2),
              List.of(lateEmployee1, lateEmployee2),
              4,
              List.of());

      // When
      List<BreakTime> breakTimes = result.breakTimes();

      // Then
      assertEquals(4, breakTimes.size());
      assertEquals(earlyEmployee1, breakTimes.get(0).employee());
      assertEquals(LocalTime.of(13, 0), breakTimes.get(0).start());
      assertEquals(LocalTime.of(14, 0), breakTimes.get(0).end());

      assertEquals(earlyEmployee2, breakTimes.get(1).employee());
      assertEquals(LocalTime.of(14, 0), breakTimes.get(1).start());
      assertEquals(LocalTime.of(15, 0), breakTimes.get(1).end());
    }
  }
}
