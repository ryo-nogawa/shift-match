package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.shiftmatch.controller.EmployeePageView.Week;
import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.MonthEmployee;
import com.example.shiftmatch.domain.MonthlyShiftResult;
import com.example.shiftmatch.domain.ShiftAssignment;
import com.example.shiftmatch.domain.ShiftSlot;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

  private static final List<ShiftSlot> SLOTS_IN_ORDER =
      List.of(
          ShiftSlot.SLOT_1,
          ShiftSlot.SLOT_1,
          ShiftSlot.SLOT_2,
          ShiftSlot.SLOT_3,
          ShiftSlot.SLOT_4,
          ShiftSlot.SLOT_5,
          ShiftSlot.SLOT_6,
          ShiftSlot.SLOT_6);

  /** 8 名（枠 1 → 6 の順）が割り当てられた成立の日。先頭の名前が枠 1 になる。 */
  private static DailyShiftResult dayWith(
      LocalDate date, String firstName, List<Employee> unassigned) {
    List<ShiftAssignment> assignments = new ArrayList<>();
    for (int i = 0; i < SLOTS_IN_ORDER.size(); i++) {
      assignments.add(assignment(i == 0 ? firstName : "f" + i, SLOTS_IN_ORDER.get(i)));
    }
    return new DailyShiftResult(
        date, 8 + unassigned.size(), Optional.of(new AssignmentResult(assignments, 0, unassigned)));
  }

  private static ShiftAssignment assignment(String name, ShiftSlot slot) {
    return new ShiftAssignment(
        Employee.working(name, LocalTime.of(7, 30), LocalTime.of(18, 30)),
        slot,
        LocalTime.of(12, 0),
        LocalTime.of(12, 45));
  }

  private static EmployeePageView.DayCell cellOn(EmployeePageView view, LocalDate date) {
    return view.weeks().stream()
        .flatMap(week -> week.days().stream())
        .filter(cell -> date.equals(cell.date()))
        .findFirst()
        .orElseThrow();
  }

  @Nested
  class マスの表示 {

    private EmployeePageView view;

    @BeforeEach
    void setUpView() {
      Employee target = Employee.working("e1", LocalTime.of(7, 30), LocalTime.of(18, 30));
      List<DailyShiftResult> days =
          List.of(
              dayWith(LocalDate.of(2026, 10, 1), "e1", List.of()),
              dayWith(LocalDate.of(2026, 10, 2), "f0", List.of(Employee.onLeave("e1"))),
              dayWith(LocalDate.of(2026, 10, 5), "f0", List.of(target)),
              new DailyShiftResult(LocalDate.of(2026, 10, 6), 5, Optional.empty()));
      view =
          createView(
              days,
              Map.of(
                  LocalDate.of(2026, 10, 12), "スポーツの日",
                  LocalDate.of(2026, 10, 10), "土曜の祝日"));
    }

    @Test
    @DisplayName("[7.3] Given: 勤務ありの営業日, When: 表示モデルを作ると, Then: 勤務時間が WORK で出る")
    void showsWorkTime() {
      EmployeePageView.DayCell cell = cellOn(view, LocalDate.of(2026, 10, 1));

      assertEquals(EmployeePageView.CellType.WORK, cell.type());
      assertEquals("7:30–14:30", cell.text());
    }

    @Test
    @DisplayName("[7.3] Given: 休みの営業日, When: 表示モデルを作ると, Then: 休 が OFF で出る")
    void showsOff() {
      EmployeePageView.DayCell cell = cellOn(view, LocalDate.of(2026, 10, 2));

      assertEquals(EmployeePageView.CellType.OFF, cell.type());
      assertEquals("休", cell.text());
    }

    @Test
    @DisplayName("[7.3] Given: 割り当てなしの営業日, When: 表示モデルを作ると, Then: – が NOT_ASSIGNED で出る")
    void showsNotAssigned() {
      EmployeePageView.DayCell cell = cellOn(view, LocalDate.of(2026, 10, 5));

      assertEquals(EmployeePageView.CellType.NOT_ASSIGNED, cell.type());
      assertEquals("–", cell.text());
    }

    @Test
    @DisplayName("[7.3] Given: 不成立の営業日, When: 表示モデルを作ると, Then: × が FAILED で出る")
    void showsFailed() {
      EmployeePageView.DayCell cell = cellOn(view, LocalDate.of(2026, 10, 6));

      assertEquals(EmployeePageView.CellType.FAILED, cell.type());
      assertEquals("×", cell.text());
    }

    @Test
    @DisplayName("[7.3] Given: 祝日でない土日, When: 表示モデルを作ると, Then: 空文字の WEEKEND になる")
    void showsWeekend() {
      for (int day : new int[] {3, 4}) {
        EmployeePageView.DayCell cell = cellOn(view, LocalDate.of(2026, 10, day));

        assertEquals(EmployeePageView.CellType.WEEKEND, cell.type());
        assertEquals("", cell.text());
      }
    }

    @Test
    @DisplayName("[7.3] Given: 平日の祝日, When: 表示モデルを作ると, Then: 祝日名の HOLIDAY になる")
    void showsWeekdayHoliday() {
      EmployeePageView.DayCell cell = cellOn(view, LocalDate.of(2026, 10, 12));

      assertEquals(EmployeePageView.CellType.HOLIDAY, cell.type());
      assertEquals("スポーツの日", cell.text());
    }

    @Test
    @DisplayName("[7.3] Given: 土曜の祝日, When: 表示モデルを作ると, Then: 祝日名の HOLIDAY になる")
    void showsWeekendHoliday() {
      EmployeePageView.DayCell cell = cellOn(view, LocalDate.of(2026, 10, 10));

      assertEquals(EmployeePageView.CellType.HOLIDAY, cell.type());
      assertEquals("土曜の祝日", cell.text());
    }

    @Test
    @DisplayName("[7.3] Given: 対象月以外のマス, When: 表示モデルを作ると, Then: BLANK で空文字になる")
    void showsBlank() {
      EmployeePageView.DayCell cell = view.weeks().get(0).days().get(0);

      assertEquals(EmployeePageView.CellType.BLANK, cell.type());
      assertEquals("", cell.text());
    }
  }
}
