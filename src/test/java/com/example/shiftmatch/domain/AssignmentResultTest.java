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

    @Test
    @DisplayName(
        "[T-1] Given: 早番2名・遅番2名の割り当て結果が与えられたとき, When: breakTimes()を実行すると, Then:"
            + " 全体が4件で順序が早番1人目=13:00~14:00→早番2人目=14:00~15:00→"
            + "遅番1人目=15:00~16:00→遅番2人目=16:00~17:00である")
    void allBreakTimesAreOrderedCorrectly() {
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

      // Then: 全体が4件
      assertEquals(4, breakTimes.size());

      // Then: 早番1人目
      assertEquals(earlyEmployee1, breakTimes.get(0).employee());
      assertEquals(LocalTime.of(13, 0), breakTimes.get(0).start());
      assertEquals(LocalTime.of(14, 0), breakTimes.get(0).end());

      // Then: 早番2人目
      assertEquals(earlyEmployee2, breakTimes.get(1).employee());
      assertEquals(LocalTime.of(14, 0), breakTimes.get(1).start());
      assertEquals(LocalTime.of(15, 0), breakTimes.get(1).end());

      // Then: 遅番1人目
      assertEquals(lateEmployee1, breakTimes.get(2).employee());
      assertEquals(LocalTime.of(15, 0), breakTimes.get(2).start());
      assertEquals(LocalTime.of(16, 0), breakTimes.get(2).end());

      // Then: 遅番2人目
      assertEquals(lateEmployee2, breakTimes.get(3).employee());
      assertEquals(LocalTime.of(16, 0), breakTimes.get(3).start());
      assertEquals(LocalTime.of(17, 0), breakTimes.get(3).end());
    }

    @Test
    @DisplayName(
        "[T-1] Given: 早番2名・遅番2名の割り当て結果が与えられたとき, When: breakTimes()を実行すると, Then:"
            + " 各休憩は1時間で、終了時刻が次の開始時刻と一致し、重ならない")
    void breakTimesAreConsecutiveAndNonOverlapping() {
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

      // Then: 各休憩は1時間
      for (BreakTime breakTime : breakTimes) {
        assertEquals(1, breakTime.end().getHour() - breakTime.start().getHour());
      }

      // Then: 連続し重ならない（各休憩の終了時刻が次の開始時刻と一致）
      for (int i = 0; i < breakTimes.size() - 1; i++) {
        assertEquals(
            breakTimes.get(i).end(),
            breakTimes.get(i + 1).start(),
            "休憩 " + i + " と " + (i + 1) + " が連続していない");
      }
    }
  }
}
