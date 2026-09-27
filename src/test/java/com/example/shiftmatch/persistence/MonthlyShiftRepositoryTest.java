package com.example.shiftmatch.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.MonthEmployee;
import com.example.shiftmatch.domain.MonthlyShiftInput;
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
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
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
          "saved_input_weekday_shift",
          "saved_input_off_day",
          "saved_input_employee");

  @Autowired private MonthlyShiftRepository repository;
  @Autowired private JdbcClient jdbcClient;

  @BeforeEach
  void clearTables() {
    SAVED_TABLES.forEach(table -> jdbcClient.sql("DELETE FROM " + table).update());
  }

  private static MonthEmployee fullTime(String name) {
    return new MonthEmployee(name, EmploymentType.FULL_TIME);
  }

  private static EmployeeProfile profile(String name, EmploymentType type, boolean mondayOff) {
    return partTimeProfile(name, type, mondayOff ? Set.of(DayOfWeek.MONDAY) : Set.of());
  }

  private static EmployeeProfile partTimeProfile(
      String name, EmploymentType type, Set<DayOfWeek> offDays) {
    return new EmployeeProfile(name, type, offDays);
  }

  private static EmployeeProfile weekdayShiftProfile(
      String name, EmploymentType type, Map<DayOfWeek, DailyWish> baseShifts) {
    return new EmployeeProfile(name, type, baseShifts, Set.of());
  }

  private static EmployeeProfile weekdayShiftProfile(
      String name,
      EmploymentType type,
      Map<DayOfWeek, DailyWish> baseShifts,
      Set<DayOfWeek> offDays) {
    return new EmployeeProfile(name, type, baseShifts, offDays);
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

      @Test
      @DisplayName("[F-7] Given: パートの複数の曜日休みを保存したとき, When: 復元すると, Then: 曜日休みが一致する")
      void restoresPartTimeOffDays() {
        Set<DayOfWeek> offDays = Set.of(DayOfWeek.TUESDAY, DayOfWeek.FRIDAY);

        repository.saveInput(
            List.of(partTimeProfile("佐藤", EmploymentType.PART_TIME, offDays)),
            YearMonth.of(2026, 10));

        assertEquals(offDays, repository.findEmployees().get(0).offDays());
      }

      @Test
      @DisplayName("[F-7] Given: 曜日休みを保存したあと別の曜日休みで保存し直したとき, When: 復元すると, Then: 以前の曜日休みは残らない")
      void overwritesPreviousOffDays() {
        repository.saveInput(
            List.of(
                partTimeProfile(
                    "佐藤", EmploymentType.PART_TIME, Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY))),
            YearMonth.of(2026, 10));

        repository.saveInput(
            List.of(partTimeProfile("佐藤", EmploymentType.PART_TIME, Set.of(DayOfWeek.WEDNESDAY))),
            YearMonth.of(2026, 10));

        assertEquals(Set.of(DayOfWeek.WEDNESDAY), repository.findEmployees().get(0).offDays());
      }

      @Test
      @DisplayName("[F-7] Given: 曜日休みの行がない従業員（旧データ相当）, When: 復元すると, Then: 曜日休みは空になる")
      void restoresEmptyOffDaysWhenNoRows() {
        jdbcClient
            .sql(
                "INSERT INTO saved_input_employee (row_index, name, employment_type)"
                    + " VALUES (0, '佐藤', 'PART_TIME')")
            .update();

        assertTrue(repository.findEmployees().get(0).offDays().isEmpty());
      }

      @Test
      @DisplayName("[F-7] Given: 曜日ごとに異なる基本シフトを保存したとき, When: 復元すると, Then: 曜日ごとの基本シフトが一致する")
      void restoresWeekdayShiftsPerDay() {
        Map<DayOfWeek, DailyWish> baseShifts = new EnumMap<>(DayOfWeek.class);
        baseShifts.put(
            DayOfWeek.MONDAY, new DailyWish(false, LocalTime.of(9, 0), LocalTime.of(17, 0)));
        baseShifts.put(
            DayOfWeek.TUESDAY, new DailyWish(false, LocalTime.of(8, 0), LocalTime.of(16, 0)));
        baseShifts.put(
            DayOfWeek.WEDNESDAY, new DailyWish(false, LocalTime.of(7, 30), LocalTime.of(18, 30)));
        baseShifts.put(
            DayOfWeek.THURSDAY, new DailyWish(false, LocalTime.of(9, 30), LocalTime.of(17, 30)));
        baseShifts.put(
            DayOfWeek.FRIDAY, new DailyWish(false, LocalTime.of(8, 30), LocalTime.of(16, 30)));
        List<EmployeeProfile> employees =
            List.of(weekdayShiftProfile("佐藤", EmploymentType.FULL_TIME, baseShifts));

        repository.saveInput(employees, YearMonth.of(2026, 10));

        assertEquals(employees, repository.findEmployees());
      }

      @Test
      @DisplayName("[F-7] Given: パートの曜日休みの曜日に基本シフトがあるとき, When: 保存すると, Then: その曜日の行は保存されない")
      void doesNotSaveBaseShiftForOffDay() {
        Map<DayOfWeek, DailyWish> baseShifts = new EnumMap<>(DayOfWeek.class);
        baseShifts.put(
            DayOfWeek.MONDAY, new DailyWish(false, LocalTime.of(9, 0), LocalTime.of(17, 0)));
        List<EmployeeProfile> employees =
            List.of(
                weekdayShiftProfile(
                    "佐藤", EmploymentType.PART_TIME, baseShifts, Set.of(DayOfWeek.MONDAY)));

        repository.saveInput(employees, YearMonth.of(2026, 10));

        Integer count =
            jdbcClient
                .sql(
                    "SELECT COUNT(*) FROM saved_input_weekday_shift WHERE row_index = 0 AND"
                        + " day_index = 0")
                .query(Integer.class)
                .single();
        assertEquals(0, count);
      }

      @Test
      @DisplayName("[F-7] Given: 基本シフトの行がない曜日（旧データ）, When: 復元すると, Then: 7:30〜18:30 で復元される")
      void restoresDefaultShiftWhenNoRowForDay() {
        jdbcClient
            .sql(
                "INSERT INTO saved_input_employee (row_index, name, employment_type)"
                    + " VALUES (0, '佐藤', 'FULL_TIME')")
            .update();

        DailyWish monday = repository.findEmployees().get(0).baseShifts().get(DayOfWeek.MONDAY);

        assertEquals(new DailyWish(false, LocalTime.of(7, 30), LocalTime.of(18, 30)), monday);
      }

      @Test
      @DisplayName("[F-7] Given: 基本シフトを保存したあと別の基本シフトで保存し直したとき, When: 復元すると, Then: 以前の基本シフトは残らない")
      void overwritesPreviousBaseShifts() {
        Map<DayOfWeek, DailyWish> first = new EnumMap<>(DayOfWeek.class);
        first.put(DayOfWeek.MONDAY, new DailyWish(false, LocalTime.of(9, 0), LocalTime.of(17, 0)));
        repository.saveInput(
            List.of(weekdayShiftProfile("佐藤", EmploymentType.FULL_TIME, first)),
            YearMonth.of(2026, 10));

        Map<DayOfWeek, DailyWish> second = new EnumMap<>(DayOfWeek.class);
        second.put(
            DayOfWeek.MONDAY, new DailyWish(false, LocalTime.of(10, 0), LocalTime.of(18, 0)));
        repository.saveInput(
            List.of(weekdayShiftProfile("佐藤", EmploymentType.FULL_TIME, second)),
            YearMonth.of(2026, 10));

        assertEquals(
            new DailyWish(false, LocalTime.of(10, 0), LocalTime.of(18, 0)),
            repository.findEmployees().get(0).baseShifts().get(DayOfWeek.MONDAY));
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
        List<MonthEmployee> names =
            List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K").stream()
                .map(name -> new MonthEmployee(name, EmploymentType.FULL_TIME))
                .toList();

        repository.saveShift(result, names);

        SavedMonthlyShift saved = repository.findShift(YearMonth.of(2026, 10)).orElseThrow();
        assertEquals(result, saved.result());
        assertEquals(names, saved.employees());
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
      @DisplayName("[F-7][8.4節] Given: 区分の異なる従業員, When: 保存して復元すると, Then: 氏名と区分が入力順で一致する")
      void restoresEmployeesWithEmploymentTypeInOrder() {
        List<MonthEmployee> employees =
            List.of(
                new MonthEmployee("A", EmploymentType.MANAGER),
                new MonthEmployee("B", EmploymentType.PART_TIME),
                new MonthEmployee("C", EmploymentType.FULL_TIME));
        MonthlyShiftResult result =
            monthOf(
                YearMonth.of(2026, 10),
                new DailyShiftResult(LocalDate.of(2026, 10, 1), 2, Optional.empty()));

        repository.saveShift(result, employees);

        assertEquals(
            employees, repository.findShift(YearMonth.of(2026, 10)).orElseThrow().employees());
      }

      @Test
      @DisplayName("[F-7][8.4節] Given: 区分の列がない既存の行, When: 復元すると, Then: 常勤として扱う")
      void treatsRowWithoutEmploymentTypeAsFullTime() {
        jdbcClient
            .sql(
                "INSERT INTO saved_month_employee (target_month, row_index, name)"
                    + " VALUES ('2026-10', 0, 'A')")
            .update();

        assertEquals(
            List.of(new MonthEmployee("A", EmploymentType.FULL_TIME)),
            repository.findShift(YearMonth.of(2026, 10)).orElseThrow().employees());
      }

      @Test
      @DisplayName("[F-7][8.4節] Given: 他の月の決定シフトがあるとき, When: 対象月を保存すると, Then: 他の月の分が残る")
      void keepsShiftsOfOtherMonths() {
        MonthlyShiftResult september =
            monthOf(YearMonth.of(2026, 9), successDay(LocalDate.of(2026, 9, 1), employees()));
        MonthlyShiftResult october =
            monthOf(YearMonth.of(2026, 10), successDay(LocalDate.of(2026, 10, 1), employees()));
        repository.saveShift(september, List.of(fullTime("A")));

        repository.saveShift(october, List.of(fullTime("B")));

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
        repository.saveShift(first, List.of(fullTime("A"), fullTime("B")));

        repository.saveShift(second, List.of(fullTime("C")));

        SavedMonthlyShift saved = repository.findShift(YearMonth.of(2026, 10)).orElseThrow();
        assertEquals(second, saved.result());
        assertEquals(List.of(fullTime("C")), saved.employees());
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

        repository.saveShift(result, List.of(fullTime("A")));

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

  @Nested
  @DisplayName("年単位の保存")
  class SaveByYear {

    private MonthlyShiftInput input(YearMonth month, String employeeName, LocalDate adjustDate) {
      return new MonthlyShiftInput(
          month,
          List.of(profile(employeeName, EmploymentType.FULL_TIME, false)),
          List.of(
              new ShiftAdjustment(
                  adjustDate,
                  employeeName,
                  new DailyWish(false, LocalTime.of(9, 0), LocalTime.of(17, 0)))));
    }

    private MonthlyShiftResult result(YearMonth month, LocalDate date) {
      return new MonthlyShiftResult(
          month, List.of(new DailyShiftResult(date, 3, Optional.empty())));
    }

    @Nested
    class 正常系 {

      @Test
      @DisplayName("[F-7][8.4節] Given: 前の年を保存済みのとき, When: 別の年を保存すると, Then: 前の年の個別変更と決定シフトが消える")
      void removesPreviousYearWhenAnotherYearIsSaved() {
        YearMonth december = YearMonth.of(2026, 12);
        YearMonth january = YearMonth.of(2027, 1);
        repository.save(
            input(december, "佐藤", LocalDate.of(2026, 12, 1)),
            result(december, LocalDate.of(2026, 12, 1)),
            List.of(fullTime("佐藤")));

        repository.save(
            input(january, "鈴木", LocalDate.of(2027, 1, 4)),
            result(january, LocalDate.of(2027, 1, 4)),
            List.of(fullTime("鈴木")));

        assertTrue(repository.findShift(december).isEmpty());
        assertEquals(1, repository.findAdjustments().size());
        assertEquals(LocalDate.of(2027, 1, 4), repository.findAdjustments().get(0).date());
        assertTrue(repository.findShift(january).isPresent());
        assertEquals(Optional.of(january), repository.findLastTargetMonth());
      }

      @Test
      @DisplayName("[F-7][8.4節] Given: 同じ年の別の月を保存済みのとき, When: 別の月を保存すると, Then: 前の月は残る")
      void keepsOtherMonthOfSameYear() {
        YearMonth october = YearMonth.of(2026, 10);
        YearMonth november = YearMonth.of(2026, 11);
        repository.save(
            input(october, "佐藤", LocalDate.of(2026, 10, 1)),
            result(october, LocalDate.of(2026, 10, 1)),
            List.of(fullTime("佐藤")));

        repository.save(
            input(november, "佐藤", LocalDate.of(2026, 11, 2)),
            result(november, LocalDate.of(2026, 11, 2)),
            List.of(fullTime("佐藤")));

        assertTrue(repository.findShift(october).isPresent());
        assertTrue(repository.findShift(november).isPresent());
        assertEquals(2, repository.findAdjustments().size());
      }
    }

    @Nested
    class 異常系 {

      @Test
      @DisplayName(
          "[F-7][8.4節] Given: 前の年を保存済みで途中の保存が失敗するとき, When: 別の年を保存すると, Then: ロールバックされ前の年が残る")
      void rollsBackWhenSaveFails() {
        YearMonth december = YearMonth.of(2026, 12);
        YearMonth january = YearMonth.of(2027, 1);
        repository.save(
            input(december, "佐藤", LocalDate.of(2026, 12, 1)),
            result(december, LocalDate.of(2026, 12, 1)),
            List.of(fullTime("佐藤")));
        String tooLongName = "あ".repeat(256);

        assertThrows(
            DataAccessException.class,
            () ->
                repository.save(
                    input(january, tooLongName, LocalDate.of(2027, 1, 4)),
                    result(january, LocalDate.of(2027, 1, 4)),
                    List.of(fullTime(tooLongName))));

        assertTrue(repository.findShift(december).isPresent());
        assertEquals(1, repository.findAdjustments().size());
        assertEquals(LocalDate.of(2026, 12, 1), repository.findAdjustments().get(0).date());
        assertEquals(Optional.of(december), repository.findLastTargetMonth());
      }
    }
  }
}
