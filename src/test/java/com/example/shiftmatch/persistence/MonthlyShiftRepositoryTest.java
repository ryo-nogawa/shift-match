package com.example.shiftmatch.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.EmploymentType;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.YearMonth;
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
}
