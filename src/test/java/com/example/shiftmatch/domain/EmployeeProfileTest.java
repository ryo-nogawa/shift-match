package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EmployeeProfile")
class EmployeeProfileTest {

  @Nested
  class 正常系 {

    @Test
    @DisplayName("[F-1] Given: パートと曜日休み, When: EmployeeProfile を作成すると, Then: offDays が保持される")
    void partTimeKeepsOffDays() {
      EmployeeProfile profile =
          new EmployeeProfile(
              "Taro", EmploymentType.PART_TIME, Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));

      assertEquals(Set.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), profile.offDays());
    }

    @Test
    @DisplayName("[F-1] Given: パートの曜日休みのセット, When: 作成後に元のセットを変更すると, Then: EmployeeProfile に反映されない")
    void offDaysAreImmutable() {
      Set<DayOfWeek> offDays = new HashSet<>(Set.of(DayOfWeek.MONDAY));
      EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.PART_TIME, offDays);

      offDays.add(DayOfWeek.FRIDAY);

      assertEquals(Set.of(DayOfWeek.MONDAY), profile.offDays());
    }

    @Test
    @DisplayName("[F-1] Given: 常勤・管理職に曜日休み, When: EmployeeProfile を作成すると, Then: offDays は空になる")
    void nonPartTimeIgnoresOffDays() {
      EmployeeProfile fullTime =
          new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of(DayOfWeek.MONDAY));
      EmployeeProfile manager =
          new EmployeeProfile("Hanako", EmploymentType.MANAGER, Set.of(DayOfWeek.FRIDAY));

      assertTrue(fullTime.offDays().isEmpty());
      assertTrue(manager.offDays().isEmpty());
    }

    @Test
    @DisplayName("[F-1] Given: パートの曜日休み, When: 曜日休みの曜日で toEmployee を実行すると, Then: 休みの Employee になる")
    void partTimeOffDayBecomesOnLeave() {
      EmployeeProfile profile =
          new EmployeeProfile("Taro", EmploymentType.PART_TIME, Set.of(DayOfWeek.TUESDAY));

      Employee employee = profile.toEmployee(DayOfWeek.TUESDAY);

      assertEquals("Taro", employee.name());
      assertEquals(EmploymentType.PART_TIME, employee.employmentType());
      assertTrue(employee.off());
      assertNull(employee.start());
      assertNull(employee.end());
    }

    @Test
    @DisplayName(
        "[F-1] Given: パートの曜日休み, When: 曜日休みでない曜日で toEmployee を実行すると, Then: 7:30〜18:30 で出勤の Employee"
            + " になる")
    void nonOffDayBecomesWorkingWithDefaultTimeRange() {
      EmployeeProfile profile =
          new EmployeeProfile("Taro", EmploymentType.PART_TIME, Set.of(DayOfWeek.TUESDAY));

      Employee employee = profile.toEmployee(DayOfWeek.MONDAY);

      assertFalse(employee.off());
      assertEquals(LocalTime.of(7, 30), employee.start());
      assertEquals(LocalTime.of(18, 30), employee.end());
    }
  }
}
