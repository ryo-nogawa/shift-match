package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("WeekGrouper")
class WeekGrouperTest {

  private final WeekGrouper grouper = new WeekGrouper();

  private static List<LocalDate> weekdaysOf(int year, int month, LocalDate... holidays) {
    List<LocalDate> holidayList = List.of(holidays);
    List<LocalDate> days = new ArrayList<>();
    LocalDate date = LocalDate.of(year, month, 1);
    while (date.getMonthValue() == month) {
      boolean weekend =
          date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
      if (!weekend && !holidayList.contains(date)) {
        days.add(date);
      }
      date = date.plusDays(1);
    }
    return days;
  }

  @Nested
  class 正常系 {

    @Test
    @DisplayName("[H-4] Given: 2026 年 10 月の営業日, When: group すると, Then: 週の数と先頭日が正しい")
    void groupsOctober2026IntoFiveWeeks() {
      List<List<LocalDate>> weeks = grouper.group(weekdaysOf(2026, 10));

      assertEquals(5, weeks.size());
      assertEquals(LocalDate.of(2026, 10, 1), weeks.get(0).get(0));
      assertEquals(LocalDate.of(2026, 10, 5), weeks.get(1).get(0));
      assertEquals(LocalDate.of(2026, 10, 12), weeks.get(2).get(0));
      assertEquals(LocalDate.of(2026, 10, 19), weeks.get(3).get(0));
      assertEquals(LocalDate.of(2026, 10, 26), weeks.get(4).get(0));
    }

    @Test
    @DisplayName("[H-4] Given: 月の途中から始まる週と月末で終わる週, When: group すると, Then: 含まれる営業日だけの週になる")
    void partialWeeksAtMonthEdgesContainOnlyIncludedDays() {
      List<List<LocalDate>> weeks = grouper.group(weekdaysOf(2026, 10));

      assertEquals(List.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2)), weeks.get(0));
      assertEquals(5, weeks.get(4).size());
      List<List<LocalDate>> november = grouper.group(weekdaysOf(2026, 11));
      assertEquals(List.of(LocalDate.of(2026, 11, 30)), november.get(november.size() - 1));
    }

    @Test
    @DisplayName("[H-4] Given: 祝日が抜けた週, When: group すると, Then: その週の日数が減る")
    void holidayReducesWeekSize() {
      List<List<LocalDate>> weeks = grouper.group(weekdaysOf(2026, 10, LocalDate.of(2026, 10, 12)));

      assertEquals(4, weeks.get(2).size());
      assertEquals(LocalDate.of(2026, 10, 13), weeks.get(2).get(0));
    }
  }

  @Nested
  class 境界値 {

    @Test
    @DisplayName("[H-4] Given: 空のリスト, When: group すると, Then: 空を返す")
    void emptyInputReturnsEmpty() {
      assertTrue(grouper.group(List.of()).isEmpty());
    }

    @Test
    @DisplayName("[H-4] Given: 1 週すべてが祝日, When: group すると, Then: その週は含まれない")
    void fullyHolidayWeekIsSkipped() {
      List<LocalDate> days = List.of(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 12));

      assertEquals(2, grouper.group(days).size());
      assertEquals(List.of(LocalDate.of(2026, 10, 2)), grouper.group(days).get(0));
    }
  }
}
