package com.example.shiftmatch.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.MonthlyShiftResult;
import com.example.shiftmatch.domain.ShiftAdjustment;
import com.example.shiftmatch.service.ShiftAssignmentService;
import com.example.shiftmatch.service.ShiftAssignmentServiceImpl;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;

/** MonthlyShiftRepository のテスト。 */
@SpringBootTest(properties = "holiday.refresh-on-startup=false")
@DisplayName("月間シフトの保存リポジトリ")
class MonthlyShiftRepositoryTest {

  private static final List<String> SAVED_TABLES =
      List.of(
          "saved_day_unassigned",
          "saved_day_assignment",
          "saved_day",
          "saved_month_employee",
          "saved_adjustment",
          "saved_input_meta",
          "saved_input_base_shift",
          "saved_input_employee");

  @Autowired private MonthlyShiftRepository repository;
  @Autowired private JdbcClient jdbcClient;

  @BeforeEach
  void clearTables() {
    SAVED_TABLES.forEach(table -> jdbcClient.sql("DELETE FROM " + table).update());
  }

  private static EmployeeProfile profile(String name, EmploymentType type, boolean mondayOff) {
    Map<DayOfWeek, DailyWish> shifts = new EnumMap<>(DayOfWeek.class);
    for (DayOfWeek day :
        List.of(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY)) {
      shifts.put(day, new DailyWish(false, LocalTime.of(9, 0), LocalTime.of(17, 30)));
    }
    if (mondayOff) {
      shifts.put(DayOfWeek.MONDAY, new DailyWish(true, null, null));
    }
    return new EmployeeProfile(name, type, shifts);
  }

  @Nested
  @DisplayName("従業員入力")
  class EmployeeInput {

    @Nested
    class 正常系 {

      @Test
      @DisplayName("[F-7] Given: 従業員を保存したとき, When: 復元すると, Then: 並び順・区分・休み・時間帯が一致する")
      void restoresEmployeesInOrder() {
        List<EmployeeProfile> employees =
            List.of(
                profile("佐藤", EmploymentType.PART_TIME, true),
                profile("鈴木", EmploymentType.MANAGER, false),
                profile("田中", EmploymentType.FULL_TIME, false));

        repository.saveInput(employees, YearMonth.of(2026, 10));

        assertEquals(employees, repository.findEmployees());
      }

      @Test
      @DisplayName("[F-7] Given: 名前が空の行があるとき, When: 保存すると, Then: その行は保存されない")
      void skipsBlankNames() {
        repository.saveInput(
            List.of(
                profile("佐藤", EmploymentType.FULL_TIME, false),
                profile("", EmploymentType.FULL_TIME, false),
                profile("鈴木", EmploymentType.FULL_TIME, false)),
            YearMonth.of(2026, 10));

        assertEquals(
            List.of("佐藤", "鈴木"), repository.findEmployees().stream().map(e -> e.name()).toList());
      }

      @Test
      @DisplayName("[F-7][8.4節] Given: 2 回保存したとき, When: 復元すると, Then: 最新の 1 組だけが残る")
      void keepsOnlyLatestInput() {
        repository.saveInput(
            List.of(
                profile("佐藤", EmploymentType.FULL_TIME, false),
                profile("鈴木", EmploymentType.FULL_TIME, false)),
            YearMonth.of(2026, 10));
        List<EmployeeProfile> latest = List.of(profile("田中", EmploymentType.PART_TIME, true));

        repository.saveInput(latest, YearMonth.of(2026, 11));

        assertEquals(latest, repository.findEmployees());
      }
    }

    @Nested
    class 異常系 {

      @Test
      @DisplayName("[F-7] Given: 何も保存していないとき, When: 従業員を復元すると, Then: 空のリストが返る")
      void returnsEmptyWhenNothingSaved() {
        assertTrue(repository.findEmployees().isEmpty());
      }
    }
  }

  @Nested
  @DisplayName("最後の対象月")
  class LastTargetMonth {

    @Test
    @DisplayName("[F-7][8.1節] Given: 何も保存していないとき, When: 最後の対象月を取得すると, Then: 空が返る")
    void returnsEmptyWhenNothingSaved() {
      assertTrue(repository.findLastTargetMonth().isEmpty());
    }

    @Test
    @DisplayName("[F-7][8.1節] Given: 1 回保存したとき, When: 最後の対象月を取得すると, Then: その月が返る")
    void returnsMonthAfterOneSave() {
      repository.saveInput(List.of(), YearMonth.of(2026, 10));

      assertEquals(Optional.of(YearMonth.of(2026, 10)), repository.findLastTargetMonth());
    }

    @Test
    @DisplayName("[F-7][8.1節] Given: 2 回保存したとき, When: 最後の対象月を取得すると, Then: 2 回目の月が返る")
    void returnsLatestMonthAfterTwoSaves() {
      repository.saveInput(List.of(), YearMonth.of(2026, 10));
      repository.saveInput(List.of(), YearMonth.of(2026, 11));

      assertEquals(Optional.of(YearMonth.of(2026, 11)), repository.findLastTargetMonth());
    }
  }

  @Nested
  @DisplayName("個別変更")
  class Adjustments {

    private ShiftAdjustment adjustment(LocalDate date, String name, DailyWish wish) {
      return new ShiftAdjustment(date, name, wish);
    }

    private DailyWish working(int startHour, int endHour) {
      return new DailyWish(false, LocalTime.of(startHour, 0), LocalTime.of(endHour, 0));
    }

    @Nested
    class 正常系 {

      @Test
      @DisplayName("[F-7][8.4節] Given: 個別変更を保存したとき, When: 復元すると, Then: 休み・時間帯が日付・従業員名順で一致する")
      void restoresAdjustmentsSortedByDateAndName() {
        ShiftAdjustment off =
            adjustment(LocalDate.of(2026, 10, 2), "佐藤", new DailyWish(true, null, null));
        ShiftAdjustment work = adjustment(LocalDate.of(2026, 10, 1), "鈴木", working(9, 17));

        repository.saveAdjustments(YearMonth.of(2026, 10), List.of(off, work));

        assertEquals(List.of(work, off), repository.findAdjustments());
      }

      @Test
      @DisplayName("[F-7][8.4節] Given: 他の月の個別変更があるとき, When: 対象月を保存すると, Then: 他の月の分が残る")
      void keepsAdjustmentsOfOtherMonths() {
        ShiftAdjustment september = adjustment(LocalDate.of(2026, 9, 30), "佐藤", working(9, 17));
        ShiftAdjustment october = adjustment(LocalDate.of(2026, 10, 1), "佐藤", working(9, 17));
        repository.saveAdjustments(YearMonth.of(2026, 9), List.of(september));

        repository.saveAdjustments(YearMonth.of(2026, 10), List.of(october));

        assertEquals(List.of(september, october), repository.findAdjustments());
      }

      @Test
      @DisplayName("[F-7][8.4節] Given: 同じ日付・従業員名を再保存するとき, When: 復元すると, Then: 最新で上書きされる")
      void overwritesSameDateAndName() {
        LocalDate date = LocalDate.of(2026, 10, 1);
        repository.saveAdjustments(
            YearMonth.of(2026, 10), List.of(adjustment(date, "佐藤", working(9, 17))));
        ShiftAdjustment latest = adjustment(date, "佐藤", working(8, 16));

        repository.saveAdjustments(YearMonth.of(2026, 10), List.of(latest));

        assertEquals(List.of(latest), repository.findAdjustments());
      }

      @Test
      @DisplayName("[F-7][8.4節] Given: 送信に同じ日付・従業員名が重複するとき, When: 保存すると, Then: 後のものが採用される")
      void lastDuplicateWins() {
        LocalDate date = LocalDate.of(2026, 10, 1);
        ShiftAdjustment latest = adjustment(date, "佐藤", working(8, 16));

        repository.saveAdjustments(
            YearMonth.of(2026, 10), List.of(adjustment(date, "佐藤", working(9, 17)), latest));

        assertEquals(List.of(latest), repository.findAdjustments());
      }

      @Test
      @DisplayName("[F-7][8.4節] Given: 対象月に個別変更が保存済みのとき, When: 0 件で保存すると, Then: 対象月分が消える")
      void removesMonthWhenSavedWithNoAdjustments() {
        repository.saveAdjustments(
            YearMonth.of(2026, 10),
            List.of(adjustment(LocalDate.of(2026, 10, 1), "佐藤", working(9, 17))));

        repository.saveAdjustments(YearMonth.of(2026, 10), List.of());

        assertTrue(repository.findAdjustments().isEmpty());
      }
    }

    @Nested
    class 異常系 {

      @Test
      @DisplayName("[F-7] Given: 何も保存していないとき, When: 個別変更を復元すると, Then: 空のリストが返る")
      void returnsEmptyWhenNothingSaved() {
        assertTrue(repository.findAdjustments().isEmpty());
      }
    }
  }

  @Nested
  @DisplayName("決定したシフト")
  class DecidedShift {

    private final ShiftAssignmentService assignmentService = new ShiftAssignmentServiceImpl();

    private List<Employee> employees() {
      List<Employee> employees = new ArrayList<>();
      employees.add(Employee.working("A", EmploymentType.FULL_TIME, time(7, 30), time(14, 30)));
      employees.add(Employee.working("B", EmploymentType.PART_TIME, time(7, 30), time(15, 0)));
      employees.add(Employee.working("C", EmploymentType.MANAGER, time(8, 0), time(15, 30)));
      employees.add(Employee.working("D", EmploymentType.FULL_TIME, time(8, 30), time(16, 30)));
      employees.add(Employee.working("E", EmploymentType.FULL_TIME, time(9, 0), time(16, 30)));
      employees.add(Employee.working("F", EmploymentType.PART_TIME, time(9, 0), time(18, 0)));
      employees.add(Employee.working("G", EmploymentType.FULL_TIME, time(9, 0), time(18, 30)));
      employees.add(Employee.working("H", EmploymentType.FULL_TIME, time(9, 0), time(18, 30)));
      employees.add(Employee.working("I", EmploymentType.FULL_TIME, time(7, 30), time(18, 30)));
      employees.add(Employee.onLeave("J", EmploymentType.MANAGER));
      employees.add(Employee.working("K", EmploymentType.PART_TIME, time(10, 0), time(12, 0)));
      return employees;
    }

    private LocalTime time(int hour, int minute) {
      return LocalTime.of(hour, minute);
    }

    private AssignmentResult assign(List<Employee> employees) {
      return assignmentService.assign(employees).orElseThrow();
    }

    private MonthlyShiftResult monthOf(YearMonth month, DailyShiftResult... days) {
      return new MonthlyShiftResult(month, List.of(days));
    }

    private DailyShiftResult successDay(LocalDate date, List<Employee> employees) {
      return new DailyShiftResult(date, 9, Optional.of(assign(employees)));
    }

    @Nested
    class 正常系 {

      @Test
      @DisplayName("[F-7][8.4節] Given: 実際に算出した結果を保存したとき, When: 復元すると, Then: 結果と従業員名が等しく理由文言も一致する")
      void restoresRealAssignmentResult() {
        List<Employee> employees = employees();
        AssignmentResult original = assign(employees);
        MonthlyShiftResult result =
            monthOf(
                YearMonth.of(2026, 10),
                new DailyShiftResult(LocalDate.of(2026, 10, 1), 9, Optional.of(original)));
        List<String> names = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K");

        repository.saveShift(result, names);

        SavedMonthlyShift saved = repository.findShift(YearMonth.of(2026, 10)).orElseThrow();
        assertEquals(result, saved.result());
        assertEquals(names, saved.employeeNames());
        AssignmentResult restored = saved.result().days().get(0).assignment().orElseThrow();
        assertEquals(original.assignments(), restored.assignments());
        assertEquals(original.score(), restored.score());
        assertEquals(original.unassignedEmployees(), restored.unassignedEmployees());
        for (Employee employee : original.unassignedEmployees()) {
          assertEquals(
              original.unassignedReasonLabel(employee), restored.unassignedReasonLabel(employee));
        }
      }

      @Test
      @DisplayName("[F-7][8.4節] Given: 他の月の決定シフトがあるとき, When: 対象月を保存すると, Then: 他の月の分が残る")
      void keepsShiftsOfOtherMonths() {
        MonthlyShiftResult september =
            monthOf(YearMonth.of(2026, 9), successDay(LocalDate.of(2026, 9, 1), employees()));
        MonthlyShiftResult october =
            monthOf(YearMonth.of(2026, 10), successDay(LocalDate.of(2026, 10, 1), employees()));
        repository.saveShift(september, List.of("A"));

        repository.saveShift(october, List.of("B"));

        assertEquals(september, repository.findShift(YearMonth.of(2026, 9)).orElseThrow().result());
        assertEquals(october, repository.findShift(YearMonth.of(2026, 10)).orElseThrow().result());
      }

      @Test
      @DisplayName("[F-7][8.4節] Given: 同じ月を保存済みのとき, When: 再保存すると, Then: 置き換わる")
      void replacesSameMonth() {
        MonthlyShiftResult first =
            monthOf(
                YearMonth.of(2026, 10),
                successDay(LocalDate.of(2026, 10, 1), employees()),
                successDay(LocalDate.of(2026, 10, 2), employees()));
        MonthlyShiftResult second =
            monthOf(YearMonth.of(2026, 10), successDay(LocalDate.of(2026, 10, 5), employees()));
        repository.saveShift(first, List.of("A", "B"));

        repository.saveShift(second, List.of("C"));

        SavedMonthlyShift saved = repository.findShift(YearMonth.of(2026, 10)).orElseThrow();
        assertEquals(second, saved.result());
        assertEquals(List.of("C"), saved.employeeNames());
      }

      @Test
      @DisplayName("[F-7][8.4節] Given: 成立日と不成立日が混在するとき, When: 復元すると, Then: 勤務可能人数と日付順が一致する")
      void restoresUnsuccessfulDaysInDateOrder() {
        MonthlyShiftResult result =
            monthOf(
                YearMonth.of(2026, 10),
                successDay(LocalDate.of(2026, 10, 1), employees()),
                new DailyShiftResult(LocalDate.of(2026, 10, 2), 5, Optional.empty()),
                successDay(LocalDate.of(2026, 10, 5), employees()),
                new DailyShiftResult(LocalDate.of(2026, 10, 6), 0, Optional.empty()));

        repository.saveShift(result, List.of("A"));

        SavedMonthlyShift saved = repository.findShift(YearMonth.of(2026, 10)).orElseThrow();
        assertEquals(result, saved.result());
        assertEquals(5, saved.result().days().get(1).availableCount());
        assertTrue(saved.result().days().get(1).assignment().isEmpty());
      }
    }

    @Nested
    class 異常系 {

      @Test
      @DisplayName("[F-7][8.4節] Given: 保存がない月のとき, When: 復元すると, Then: 空が返る")
      void returnsEmptyWhenMonthNotSaved() {
        assertTrue(repository.findShift(YearMonth.of(2026, 10)).isEmpty());
      }
    }
  }
}
