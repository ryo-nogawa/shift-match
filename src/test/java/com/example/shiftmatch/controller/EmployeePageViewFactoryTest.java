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

  private static final Map<LocalDate, String> SAMPLE_HOLIDAYS =
      Map.of(
          LocalDate.of(2026, 10, 12), "スポーツの日",
          LocalDate.of(2026, 10, 10), "土曜の祝日");

  /** e1 が 10/1 と 10/30 に枠 1、10/2 に休み、10/5 に割り当てなし、10/6 に不成立の月。 */
  private static List<DailyShiftResult> sampleDays() {
    Employee target = Employee.working("e1", LocalTime.of(7, 30), LocalTime.of(18, 30));
    return List.of(
        dayWith(LocalDate.of(2026, 10, 1), "e1", List.of()),
        dayWith(LocalDate.of(2026, 10, 2), "f0", List.of(Employee.onLeave("e1"))),
        dayWith(LocalDate.of(2026, 10, 5), "f0", List.of(target)),
        new DailyShiftResult(LocalDate.of(2026, 10, 6), 5, Optional.empty()),
        dayWith(LocalDate.of(2026, 10, 30), "e1", List.of()));
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
      view = createView(sampleDays(), SAMPLE_HOLIDAYS);
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

  @Nested
  class 集計 {

    private EmployeePageView view;

    @BeforeEach
    void setUpView() {
      view = createView(sampleDays(), SAMPLE_HOLIDAYS);
    }

    @Test
    @DisplayName(
        "[7.3] Given: 月をまたぐ第 1 週と第 5 週を含む月, When: 表示モデルを作ると," + " Then: 週の合計は対象月の営業日だけの実労働時間になる")
    void totalsEachWeek() {
      List<Integer> totals = view.weeks().stream().map(week -> week.totalMinutes()).toList();

      assertEquals(List.of(375, 0, 0, 0, 375), totals);
      assertEquals("(06:15)", view.weeks().get(0).durationLabel());
      assertEquals("(00:00)", view.weeks().get(1).durationLabel());
      assertEquals("(06:15)", view.weeks().get(4).durationLabel());
    }

    @Test
    @DisplayName(
        "[7.3] Given: 勤務 2 日・休み・割り当てなし・不成立の月, When: 表示モデルを作ると," + " Then: 出勤日数・月間の合計・不成立日数が数えられる")
    void totalsMonth() {
      assertEquals(2, view.workDays());
      assertEquals(750, view.totalMinutes());
      assertEquals("(12:30)", view.durationLabel());
      assertEquals(1, view.failureCount());
    }

    @Test
    @DisplayName("[7.1] Given: 同じ入力, When: 従業員別表示と個別ページを作ると," + " Then: 出勤日数と月間の合計が一致する")
    void matchesEmployeeRow() {
      MonthlyShiftResult result = new MonthlyShiftResult(MONTH, sampleDays());

      MonthlyResultView.EmployeeRow row =
          new MonthlyResultViewFactory()
              .create(result, List.of(TARGET), SAMPLE_HOLIDAYS)
              .employeeRows()
              .get(0);

      assertEquals(row.workDays(), view.workDays());
      assertEquals(row.totalMinutes(), view.totalMinutes());
      assertEquals(row.durationLabel(), view.durationLabel());
    }
  }
}
