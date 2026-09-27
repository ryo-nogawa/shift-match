package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.ShiftAdjustment;
import com.example.shiftmatch.service.SavedInput;
import com.example.shiftmatch.service.ShiftAssignmentServiceImpl;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SavedInputFormConverter")
class SavedInputFormConverterTest {

  private static final YearMonth DEFAULT_MONTH = YearMonth.of(2026, 9);

  /** デモ用 12 名の氏名・区分・曜日休み（0＝月〜4＝金）。 */
  private record DemoRow(String name, String type, List<Integer> offDays) {}

  private static final List<DemoRow> DEMO_EMPLOYEES =
      List.of(
          new DemoRow("佐藤太郎", "FULL_TIME", List.of()),
          new DemoRow("鈴木花子", "FULL_TIME", List.of()),
          new DemoRow("高橋健一", "FULL_TIME", List.of()),
          new DemoRow("田中美咲", "FULL_TIME", List.of()),
          new DemoRow("伊藤大輔", "FULL_TIME", List.of()),
          new DemoRow("渡辺陽子", "PART_TIME", List.of(0, 2)),
          new DemoRow("山本翔太", "PART_TIME", List.of(1, 3)),
          new DemoRow("中村由美", "PART_TIME", List.of(4)),
          new DemoRow("小林誠", "MANAGER", List.of()),
          new DemoRow("加藤恵", "FULL_TIME", List.of()),
          new DemoRow("吉田拓也", "PART_TIME", List.of(0, 3)),
          new DemoRow("山田彩香", "MANAGER", List.of()));

  private final SavedInputFormConverter converter = new SavedInputFormConverter();

  private static EmployeeProfile profile(String name, EmploymentType type) {
    return new EmployeeProfile(name, type, Set.of());
  }

  private static EmployeeProfile partTimeWithOffDays(String name, Set<DayOfWeek> offDays) {
    return new EmployeeProfile(name, EmploymentType.PART_TIME, offDays);
  }

  private static SavedInput savedOf(
      List<EmployeeProfile> employees,
      List<ShiftAdjustment> adjustments,
      Optional<YearMonth> lastMonth) {
    return new SavedInput(employees, adjustments, lastMonth);
  }

  private static void assertEmptyRow(EmployeeForm row) {
    assertEquals("", row.getName());
    assertEquals("FULL_TIME", row.getEmploymentType());
    assertTrue(row.getOffDays().isEmpty());
  }

  @Nested
  class 正常系 {

    @Test
    @DisplayName(
        "[F-7][8.1節] Given: 従業員が 2 名保存済み, When: フォームに変換すると, Then: 先頭 2 行に復元され、残りは空の行で計 12 行になる")
    void restoresSavedRowsFirstAndPadsToTwelve() {
      SavedInput saved =
          savedOf(
              List.of(
                  profile("佐藤", EmploymentType.PART_TIME), profile("鈴木", EmploymentType.MANAGER)),
              List.of(),
              Optional.empty());

      ShiftForm form = converter.toForm(saved, DEFAULT_MONTH);

      assertEquals(12, form.getEmployees().size());
      EmployeeForm first = form.getEmployees().get(0);
      assertEquals("佐藤", first.getName());
      assertEquals("PART_TIME", first.getEmploymentType());
      assertEquals("MANAGER", form.getEmployees().get(1).getEmploymentType());
      assertEquals("鈴木", form.getEmployees().get(1).getName());
      for (int i = 2; i < 12; i++) {
        assertEmptyRow(form.getEmployees().get(i));
      }
    }

    @Test
    @DisplayName("[F-7] Given: パートの曜日休み（火・金）が保存済み, When: フォームに変換すると, Then: offDays が [1, 4] に復元される")
    void restoresPartTimeOffDays() {
      SavedInput saved =
          savedOf(
              List.of(partTimeWithOffDays("佐藤", Set.of(DayOfWeek.FRIDAY, DayOfWeek.TUESDAY))),
              List.of(),
              Optional.empty());

      EmployeeForm row = converter.toForm(saved, DEFAULT_MONTH).getEmployees().get(0);

      assertEquals(List.of(1, 4), row.getOffDays());
    }

    @Test
    @DisplayName("[F-7] Given: 曜日休みのない従業員が保存済み, When: フォームに変換すると, Then: offDays は空になる")
    void restoresEmptyOffDaysWhenNoneSaved() {
      SavedInput saved =
          savedOf(List.of(profile("佐藤", EmploymentType.PART_TIME)), List.of(), Optional.empty());

      EmployeeForm row = converter.toForm(saved, DEFAULT_MONTH).getEmployees().get(0);

      assertTrue(row.getOffDays().isEmpty());
    }

    @Test
    @DisplayName(
        "[F-7][8.1節] Given: 保存済みの従業員が 0 件, When: フォームに変換すると, Then: デモ用 12 名（氏名・区分・曜日休み）と既定の対象月になる")
    void returnsDemoEmployeesAndDefaultMonthWhenNothingSaved() {
      ShiftForm form =
          converter.toForm(savedOf(List.of(), List.of(), Optional.empty()), DEFAULT_MONTH);

      assertEquals("2026-09", form.getTargetMonth());
      assertEquals(DEMO_EMPLOYEES.size(), form.getEmployees().size());
      for (int i = 0; i < DEMO_EMPLOYEES.size(); i++) {
        DemoRow expected = DEMO_EMPLOYEES.get(i);
        EmployeeForm row = form.getEmployees().get(i);
        assertEquals(expected.name(), row.getName());
        assertEquals(expected.type(), row.getEmploymentType());
        assertEquals(expected.offDays(), row.getOffDays());
      }
      assertTrue(form.getAdjustments().isEmpty());
    }

    @Test
    @DisplayName(
        "[F-7][8.1節] Given: 保存済みの従業員が 1 名, When: フォームに変換すると," + " Then: デモ用データは混ざらず、残りは空の行になる")
    void doesNotMixDemoEmployeesWhenOneSaved() {
      SavedInput saved =
          savedOf(List.of(profile("試験花子", EmploymentType.PART_TIME)), List.of(), Optional.empty());

      ShiftForm form = converter.toForm(saved, DEFAULT_MONTH);

      assertEquals(12, form.getEmployees().size());
      assertEquals("試験花子", form.getEmployees().get(0).getName());
      assertEquals("PART_TIME", form.getEmployees().get(0).getEmploymentType());
      for (int i = 1; i < 12; i++) {
        assertEmptyRow(form.getEmployees().get(i));
      }
      List<String> demoNames = DEMO_EMPLOYEES.stream().map(row -> row.name()).toList();
      form.getEmployees().forEach(row -> assertFalse(demoNames.contains(row.getName())));
    }

    @Test
    @DisplayName("[8.1節] Given: デモ用 12 名, When: 月〜金の各曜日を数えると, Then: 休みでない従業員が 8 名以上いる")
    void demoEmployeesHaveAtLeastEightWorkingOnEveryWeekday() {
      ShiftForm form =
          converter.toForm(savedOf(List.of(), List.of(), Optional.empty()), DEFAULT_MONTH);

      for (int dayIndex = 0; dayIndex < 5; dayIndex++) {
        int working = 0;
        for (EmployeeForm row : form.getEmployees()) {
          if (!row.getOffDays().contains(dayIndex)) {
            working++;
          }
        }
        assertTrue(working >= 8, "曜日インデックス " + dayIndex + " の出勤可能人数: " + working);
      }
    }

    @Test
    @DisplayName("[8.1節][H-1] Given: デモ用 12 名, When: 月〜金の各曜日で割り当てると, Then: すべての曜日で案が成立する")
    void demoEmployeesAssignableOnEveryWeekday() {
      ShiftForm form =
          converter.toForm(savedOf(List.of(), List.of(), Optional.empty()), DEFAULT_MONTH);
      ShiftAssignmentServiceImpl service = new ShiftAssignmentServiceImpl();

      for (int dayIndex = 0; dayIndex < 5; dayIndex++) {
        List<Employee> employees = new ArrayList<>();
        for (EmployeeForm row : form.getEmployees()) {
          if (!row.getOffDays().contains(dayIndex)) {
            employees.add(
                Employee.working(
                    row.getName(),
                    EmploymentType.valueOf(row.getEmploymentType()),
                    LocalTime.of(7, 30),
                    LocalTime.of(18, 30)));
          }
        }
        assertTrue(service.assign(employees).isPresent(), "曜日インデックス " + dayIndex);
      }
    }

    @Test
    @DisplayName("[F-7][8.1節] Given: 最後の対象月が保存済み, When: フォームに変換すると, Then: 対象月が既定より優先される")
    void usesLastTargetMonthAsDefault() {
      ShiftForm form =
          converter.toForm(
              savedOf(List.of(), List.of(), Optional.of(YearMonth.of(2026, 11))), DEFAULT_MONTH);

      assertEquals("2026-11", form.getTargetMonth());
    }

    @Test
    @DisplayName(
        "[F-7][8.1節] Given: 個別変更が保存済み, When: フォームに変換すると, Then: 日付は yyyy-MM-dd、休みと時間帯が復元される")
    void restoresAdjustments() {
      List<ShiftAdjustment> adjustments =
          List.of(
              new ShiftAdjustment(
                  LocalDate.of(2026, 10, 1),
                  "佐藤",
                  new DailyWish(false, LocalTime.of(9, 0), LocalTime.of(17, 0))),
              new ShiftAdjustment(
                  LocalDate.of(2026, 10, 2), "鈴木", new DailyWish(true, null, null)));

      List<AdjustmentForm> forms =
          converter
              .toForm(savedOf(List.of(), adjustments, Optional.empty()), DEFAULT_MONTH)
              .getAdjustments();

      assertEquals(2, forms.size());
      assertEquals("2026-10-01", forms.get(0).getDate());
      assertEquals("佐藤", forms.get(0).getEmployeeName());
      assertFalse(forms.get(0).isOff());
      assertEquals("09:00", forms.get(0).getStart());
      assertEquals("17:00", forms.get(0).getEnd());
      assertEquals("2026-10-02", forms.get(1).getDate());
      assertTrue(forms.get(1).isOff());
    }

    @Test
    @DisplayName("[F-7][8.1節] Given: 保存済みの従業員が 12 名を超える, When: フォームに変換すると, Then: 切り捨てず全員を復元する")
    void doesNotTruncateBeyondTwelve() {
      List<EmployeeProfile> employees = new ArrayList<>();
      for (int i = 0; i < 13; i++) {
        employees.add(profile("従業員" + i, EmploymentType.FULL_TIME));
      }

      ShiftForm form =
          converter.toForm(savedOf(employees, List.of(), Optional.empty()), DEFAULT_MONTH);

      assertEquals(13, form.getEmployees().size());
    }
  }
}
