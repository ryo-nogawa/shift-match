package com.example.shiftmatch.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("MonthlyShiftInput")
class MonthlyShiftInputTest {

  @Nested
  class 正常系 {

    @Test
    @DisplayName(
        "Given: 従業員リストを与えるとき, When: MonthlyShiftInput を作成してから元のリストを変更すると, Then: MonthlyShiftInput"
            + " に変更が反映されない")
    void employeesAreImmutable() {

      EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of());
      List<EmployeeProfile> employees = new ArrayList<>();
      employees.add(profile);

      List<ShiftAdjustment> adjustments = new ArrayList<>();
      MonthlyShiftInput input =
          new MonthlyShiftInput(YearMonth.of(2024, 9), employees, adjustments);

      // 元のリストを変更
      employees.clear();

      // MonthlyShiftInput の従業員リストは変わらない
      assertEquals(1, input.employees().size());
      assertEquals("Taro", input.employees().get(0).name());
    }

    @Test
    @DisplayName(
        "Given: 個別変更リストを与えるとき, When: MonthlyShiftInput を作成してから元のリストを変更すると, Then: MonthlyShiftInput"
            + " に変更が反映されない")
    void adjustmentsAreImmutable() {
      List<EmployeeProfile> employees = new ArrayList<>();
      List<ShiftAdjustment> adjustments = new ArrayList<>();
      LocalDate date = LocalDate.of(2024, 9, 2);
      DailyWish wish = new DailyWish(true, null, null);
      adjustments.add(new ShiftAdjustment(date, "Taro", wish));

      YearMonth month = YearMonth.of(2024, 9);
      MonthlyShiftInput input = new MonthlyShiftInput(month, employees, adjustments);

      // 元のリストを変更
      adjustments.clear();

      // MonthlyShiftInput の個別変更リストは変わらない
      assertEquals(1, input.adjustments().size());
    }
  }
}
