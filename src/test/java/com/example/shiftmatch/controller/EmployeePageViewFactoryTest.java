package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.shiftmatch.controller.EmployeePageView.Week;
import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.MonthEmployee;
import com.example.shiftmatch.domain.MonthlyShiftResult;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EmployeePageViewFactoryTest {

  private static final YearMonth MONTH = YearMonth.of(2026, 10);

  private static final MonthEmployee TARGET = new MonthEmployee("e1", EmploymentType.FULL_TIME);

  private EmployeePageViewFactory factory;

  @BeforeEach
  void setUp() {
    factory = new EmployeePageViewFactory();
  }

  private EmployeePageView createView(
      List<DailyShiftResult> days, Map<LocalDate, String> holidays) {
    return factory.create(new MonthlyShiftResult(MONTH, days), TARGET, holidays);
  }

  @Nested
  class 週の並び {

    @Test
    @DisplayName(
        "[7.3] Given: 2026 年 10 月, When: 表示モデルを作ると," + " Then: 週が 5 つで各週 7 マス、対象月以外のマスは空きになる")
    void buildsWeeksFromMondayToSunday() {
      EmployeePageView view = createView(List.of(), Map.of());

      assertEquals(MONTH, view.month());
      assertEquals("e1", view.name());
      assertEquals(EmploymentType.FULL_TIME, view.employmentType());
      assertEquals(5, view.weeks().size());
      for (Week week : view.weeks()) {
        assertEquals(7, week.days().size());
      }
      List<EmployeePageView.DayCell> first = view.weeks().get(0).days();
      assertNull(first.get(0).date());
      assertNull(first.get(1).date());
      assertNull(first.get(2).date());
      assertEquals(LocalDate.of(2026, 10, 1), first.get(3).date());
      assertEquals(LocalDate.of(2026, 10, 4), first.get(6).date());
      List<EmployeePageView.DayCell> last = view.weeks().get(4).days();
      assertEquals(LocalDate.of(2026, 10, 31), last.get(5).date());
      assertNull(last.get(6).date());
    }
  }
}
