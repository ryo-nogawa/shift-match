package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.FailureReason;
import com.example.shiftmatch.domain.MonthEmployee;
import com.example.shiftmatch.domain.MonthlyShiftResult;
import com.example.shiftmatch.domain.ShiftAssignment;
import com.example.shiftmatch.domain.ShiftSlot;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MonthlyResultViewFactoryTest {

  private static final LocalDate DAY_1 = LocalDate.of(2026, 10, 1);

  private static final LocalDate DAY_2 = LocalDate.of(2026, 10, 2);

  private static final LocalDate DAY_3 = LocalDate.of(2026, 10, 5);

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

  private MonthlyResultViewFactory factory;

  @BeforeEach
  void setUp() {
    factory = new MonthlyResultViewFactory();
  }

  private static Employee employee(String name) {
    return Employee.working(name, LocalTime.of(7, 30), LocalTime.of(18, 30));
  }

  /** 名前 e1〜e8 を枠 1 → 6 の順に割り当てた成立の日。unassigned は未出勤者。 */
  private static DailyShiftResult feasibleDay(LocalDate date, List<Employee> unassigned) {
    List<ShiftAssignment> assignments = new ArrayList<>();
    for (int i = 0; i < SLOTS_IN_ORDER.size(); i++) {
      assignments.add(
          new ShiftAssignment(
              employee("e" + (i + 1)),
              SLOTS_IN_ORDER.get(i),
              LocalTime.of(12, 0),
              LocalTime.of(12, 45)));
    }
    return new DailyShiftResult(
        date, 8 + unassigned.size(), Optional.of(new AssignmentResult(assignments, 0, unassigned)));
  }

  private static DailyShiftResult failedDay(LocalDate date, int availableCount) {
    return new DailyShiftResult(date, availableCount, Optional.empty());
  }

  private static DailyShiftResult failedDay(
      LocalDate date, int availableCount, FailureReason failureReason) {
    return new DailyShiftResult(date, availableCount, Optional.empty(), failureReason);
  }

  private static List<MonthEmployee> names(String... names) {
    return java.util.Arrays.stream(names)
        .map(name -> new MonthEmployee(name, EmploymentType.FULL_TIME))
        .toList();
  }

  private MonthlyResultView createView(
      List<DailyShiftResult> days, List<MonthEmployee> employees, Map<LocalDate, String> holidays) {
    return factory.create(
        new MonthlyShiftResult(YearMonth.of(2026, 10), days), employees, holidays);
  }

  @Nested
  class 正常系 {

    @Test
    @DisplayName(
        "[F-4] Given: 成立 2 日・不成立 1 日の結果, When: 表示モデルを作ると," + " Then: 営業日数・成立日数・不成立日数が数えられる")
    void countsBusinessDaysSuccessAndFailure() {
      MonthlyResultView view =
          createView(
              List.of(
                  feasibleDay(DAY_1, List.of()),
                  failedDay(DAY_2, 5),
                  feasibleDay(DAY_3, List.of())),
              names("e1"),
              Map.of());

      assertEquals(3, view.businessDayCount());
      assertEquals(2, view.successCount());
      assertEquals(1, view.failureCount());
      assertEquals(List.of(DAY_1, DAY_2, DAY_3), view.days().stream().map(d -> d.date()).toList());
    }

    @Test
    @DisplayName(
        "[F-4] Given: 枠 1 に 2 名・枠 2〜5 に 1 名ずつ・枠 6 に 2 名の日, When: 表示モデルを作ると,"
            + " Then: 同じ勤務時間の氏名が・でまとめられ、枠 1 → 6 の順に並ぶ")
    void groupsNamesByWorkTimeInSlotOrder() {
      MonthlyResultView view =
          createView(List.of(feasibleDay(DAY_1, List.of())), names("e1"), Map.of());

      List<MonthlyResultView.WorkGroup> groups = view.days().get(0).groups();
      assertEquals(
          List.of(
              new MonthlyResultView.WorkGroup("7:30–14:30", "e1・e2"),
              new MonthlyResultView.WorkGroup("8:00–15:30", "e3"),
              new MonthlyResultView.WorkGroup("8:30–16:30", "e4"),
              new MonthlyResultView.WorkGroup("9:00–16:30", "e5"),
              new MonthlyResultView.WorkGroup("9:00–18:00", "e6"),
              new MonthlyResultView.WorkGroup("9:00–18:30", "e7・e8")),
          groups);
      assertFalse(view.days().get(0).failed());
    }

    @Test
    @DisplayName(
        "[F-5] Given: 不成立の日, When: 表示モデルを作ると," + " Then: failed が true で勤務できる人数を持ち、勤務時間のまとまりは空になる")
    void marksFailedDayWithAvailableCount() {
      MonthlyResultView view = createView(List.of(failedDay(DAY_2, 5)), names("e1"), Map.of());

      MonthlyResultView.CalendarDay day = view.days().get(0);
      assertTrue(day.failed());
      assertEquals(5, day.availableCount());
      assertTrue(day.groups().isEmpty());
      assertEquals(FailureReason.STAFF_SHORTAGE, day.failureReason());
    }

    @Test
    @DisplayName(
        "[7.1] Given: WEEKLY_LIMITの不成立の日, When: 表示モデルを作ると, Then: CalendarDayにfailureReasonが引き継がれる")
    void carriesWeeklyLimitFailureReason() {
      MonthlyResultView view =
          createView(
              List.of(failedDay(DAY_2, 9, FailureReason.WEEKLY_LIMIT)), names("e1"), Map.of());

      MonthlyResultView.CalendarDay day = view.days().get(0);
      assertEquals(FailureReason.WEEKLY_LIMIT, day.failureReason());
      assertEquals("不成立（パートの週上限）", day.failureText());
    }

    @Test
    @DisplayName(
        "[F-4] Given: 割り当て・休み・割り当てなし・不成立の日がある, When: 表示モデルを作ると,"
            + " Then: 従業員別のセルが勤務時間・休・–・×になり、出勤日数は割り当てのあった日数になる")
    void buildsEmployeeCellsAndWorkDays() {
      Employee onLeave = Employee.onLeave("休みさん");
      Employee idle = employee("控えさん");
      MonthlyResultView view =
          createView(
              List.of(
                  feasibleDay(DAY_1, List.of(onLeave, idle)),
                  failedDay(DAY_2, 5),
                  feasibleDay(DAY_3, List.of(idle, onLeave))),
              names("e1", "e3", "休みさん", "控えさん", "e8"),
              Map.of());

      List<MonthlyResultView.EmployeeRow> rows = view.employeeRows();
      assertEquals(
          List.of("e1", "e3", "休みさん", "控えさん", "e8"), rows.stream().map(r -> r.name()).toList());
      assertEquals(List.of("7:30–14:30", "×", "7:30–14:30"), rows.get(0).cells());
      assertEquals(2, rows.get(0).workDays());
      assertEquals(List.of("8:00–15:30", "×", "8:00–15:30"), rows.get(1).cells());
      assertEquals(List.of("休", "×", "休"), rows.get(2).cells());
      assertEquals(0, rows.get(2).workDays());
      assertEquals(List.of("–", "×", "–"), rows.get(3).cells());
      assertEquals(List.of("9:00–18:30", "×", "9:00–18:30"), rows.get(4).cells());
    }

    @Test
    @DisplayName("[F-4] Given: 月〜金の祝日と土日の祝日がある, When: 表示モデルを作ると," + " Then: 祝日セルには月〜金の祝日だけが日付順で入る")
    void keepsOnlyWeekdayHolidays() {
      Map<LocalDate, String> holidays = new LinkedHashMap<>();
      holidays.put(LocalDate.of(2026, 10, 10), "土曜の祝日");
      holidays.put(LocalDate.of(2026, 10, 12), "スポーツの日");
      holidays.put(LocalDate.of(2026, 10, 18), "日曜の祝日");

      MonthlyResultView view =
          createView(List.of(feasibleDay(DAY_1, List.of())), names("e1"), holidays);

      assertEquals(
          List.of(new MonthlyResultView.HolidayCell(LocalDate.of(2026, 10, 12), "スポーツの日")),
          view.holidayCells());
    }

    @Test
    @DisplayName(
        "[F-4] Given: 月初が木曜で、月曜に祝日がある月, When: カレンダーのマスを作ると," + " Then: 先頭に月〜水の空きが入り、営業日と祝日が日付順に並ぶ")
    void arrangesCalendarCellsInDateOrderWithLeadingBlanks() {
      MonthlyResultView view =
          createView(
              List.of(
                  feasibleDay(DAY_1, List.of()),
                  failedDay(DAY_2, 5),
                  feasibleDay(DAY_3, List.of())),
              names("e1"),
              Map.of(LocalDate.of(2026, 10, 12), "スポーツの日"));

      List<MonthlyResultView.CalendarCell> cells = view.calendarCells();

      assertEquals(7, cells.size());
      assertTrue(cells.get(0).blank() && cells.get(1).blank() && cells.get(2).blank());
      assertEquals(DAY_1, cells.get(3).day().date());
      assertEquals(DAY_2, cells.get(4).day().date());
      assertEquals(DAY_3, cells.get(5).day().date());
      assertEquals("スポーツの日", cells.get(6).holiday().name());
    }

    @Test
    @DisplayName("[F-4] Given: 営業日の祝日名がない, When: 表示モデルを作ると, Then: CalendarDay の祝日名は null")
    void leavesHolidayNameNullOnBusinessDay() {
      MonthlyResultView view =
          createView(List.of(feasibleDay(DAY_1, List.of())), names("e1"), Map.of());

      assertNull(view.days().get(0).holidayName());
    }
  }

  @Nested
  class 異常系 {

    @Test
    @DisplayName("[F-4] Given: 営業日が 0 日の結果, When: 表示モデルを作ると, Then: 集計はすべて 0 で行のセルは空になる")
    void handlesEmptyMonth() {
      MonthlyResultView view = createView(List.of(), names("e1"), Map.of());

      assertEquals(0, view.businessDayCount());
      assertEquals(0, view.successCount());
      assertEquals(0, view.failureCount());
      assertTrue(view.days().isEmpty());
      assertTrue(view.employeeRows().get(0).cells().isEmpty());
      assertEquals(0, view.employeeRows().get(0).workDays());
    }
  }

  @Nested
  class 月間勤務時間 {

    @Test
    @DisplayName(
        "[F-4][7.1節] Given: 枠 1 と枠 6 に入った従業員, When: 表示モデルを作ると,"
            + " Then: 出勤日数 2・合計 885 分（375＋510）で (14:45) になる")
    void sumsWorkMinutes() {
      Employee partTime =
          new Employee(
              "e1", EmploymentType.PART_TIME, false, LocalTime.of(7, 30), LocalTime.of(18, 30));
      DailyShiftResult day1 = dayWith(DAY_1, partTime, ShiftSlot.SLOT_1);
      DailyShiftResult day3 = dayWith(DAY_3, partTime, ShiftSlot.SLOT_6);

      MonthlyResultView view =
          createView(
              List.of(day1, day3),
              List.of(new MonthEmployee("e1", EmploymentType.PART_TIME)),
              Map.of());

      MonthlyResultView.EmployeeRow row = view.employeeRows().get(0);
      assertEquals("e1", row.name());
      assertEquals(2, row.workDays());
      assertEquals(885, row.totalMinutes());
      assertEquals("(14:45)", row.durationLabel());
    }

    @Test
    @DisplayName("[F-4][7.1節] Given: 不成立の日がある, When: 表示モデルを作ると, Then: 不成立の日は 0 分で数える")
    void countsFailedDayAsZero() {
      MonthlyResultView view =
          createView(
              List.of(feasibleDay(DAY_1, List.of()), failedDay(DAY_2, 5)), names("e1"), Map.of());

      MonthlyResultView.EmployeeRow row = view.employeeRows().get(0);
      assertEquals(1, row.workDays());
      assertEquals(375, row.totalMinutes());
    }

    @Test
    @DisplayName(
        "[F-4][7.1節] Given: 全日休みの従業員, When: 表示モデルを作ると," + " Then: 行が出て 0 分・出勤日数 0 で (00:00) になる")
    void keepsRowForEmployeeOnLeaveAllDays() {
      Employee onLeave = Employee.onLeave("休みさん");
      MonthlyResultView view =
          createView(
              List.of(feasibleDay(DAY_1, List.of(onLeave)), feasibleDay(DAY_3, List.of(onLeave))),
              names("休みさん"),
              Map.of());

      MonthlyResultView.EmployeeRow row = view.employeeRows().get(0);
      assertEquals("休みさん", row.name());
      assertEquals(0, row.workDays());
      assertEquals(0, row.totalMinutes());
      assertEquals("(00:00)", row.durationLabel());
    }

    @Test
    @DisplayName("[F-4][7.1節] Given: 従業員名が複数, When: 表示モデルを作ると, Then: 入力順に並ぶ")
    void keepsInputOrder() {
      MonthlyResultView view =
          createView(List.of(feasibleDay(DAY_1, List.of())), names("e8", "e1", "e3"), Map.of());

      assertEquals(
          List.of("e8", "e1", "e3"), view.employeeRows().stream().map(r -> r.name()).toList());
      assertEquals(510, view.employeeRows().get(0).totalMinutes());
      assertEquals(405, view.employeeRows().get(2).totalMinutes());
    }

    /** person を slot の最初の枠に入れ、元の同名の人は別名 other に置き換えた成立の日を作る。 */
    private static DailyShiftResult dayWith(LocalDate date, Employee person, ShiftSlot slot) {
      List<ShiftAssignment> assignments = new ArrayList<>();
      boolean placed = false;
      for (int i = 0; i < SLOTS_IN_ORDER.size(); i++) {
        ShiftSlot current = SLOTS_IN_ORDER.get(i);
        Employee who = employee("e" + (i + 1));
        if (!placed && current == slot) {
          who = person;
          placed = true;
        } else if (who.name().equals(person.name())) {
          who = employee("other");
        }
        assignments.add(
            new ShiftAssignment(who, current, LocalTime.of(12, 0), LocalTime.of(12, 45)));
      }
      return new DailyShiftResult(
          date, 8, Optional.of(new AssignmentResult(assignments, 0, List.of())));
    }
  }
}
