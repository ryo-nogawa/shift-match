package com.example.shiftmatch.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.ShiftAdjustment;
import com.example.shiftmatch.service.SavedInput;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SavedInputFormConverter")
class SavedInputFormConverterTest {

  private static final YearMonth DEFAULT_MONTH = YearMonth.of(2026, 9);

  private final SavedInputFormConverter converter = new SavedInputFormConverter();

  private static EmployeeProfile profile(String name, EmploymentType type) {
    Map<DayOfWeek, DailyWish> shifts = new EnumMap<>(DayOfWeek.class);
    shifts.put(DayOfWeek.MONDAY, new DailyWish(true, null, null));
    shifts.put(DayOfWeek.TUESDAY, new DailyWish(false, LocalTime.of(8, 0), LocalTime.of(17, 30)));
    shifts.put(DayOfWeek.WEDNESDAY, new DailyWish(false, LocalTime.of(9, 0), LocalTime.of(18, 0)));
    shifts.put(DayOfWeek.THURSDAY, new DailyWish(false, LocalTime.of(7, 30), LocalTime.of(14, 30)));
    shifts.put(DayOfWeek.FRIDAY, new DailyWish(false, LocalTime.of(10, 0), LocalTime.of(18, 30)));
    return new EmployeeProfile(name, type, shifts);
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
    assertEquals(5, row.getDays().size());
    for (DayForm day : row.getDays()) {
      assertEquals("07:30", day.getStart());
      assertEquals("18:30", day.getEnd());
    }
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
    @DisplayName(
        "[F-7][8.1節] Given: 休みの曜日が混じった基本シフトが保存済み, When: フォームに変換すると,"
            + " Then: 休みの曜日は 07:30〜18:30 で復元され、他の曜日は保存された時間帯のまま")
    void restoresOffDayAsDefaultTimeRange() {
      Map<DayOfWeek, DailyWish> shifts = new EnumMap<>(DayOfWeek.class);
      shifts.put(DayOfWeek.MONDAY, new DailyWish(false, LocalTime.of(8, 0), LocalTime.of(17, 0)));
      shifts.put(DayOfWeek.TUESDAY, new DailyWish(false, LocalTime.of(9, 0), LocalTime.of(16, 0)));
      shifts.put(
          DayOfWeek.WEDNESDAY, new DailyWish(true, LocalTime.of(10, 0), LocalTime.of(15, 0)));
      shifts.put(DayOfWeek.THURSDAY, new DailyWish(true, null, null));
      shifts.put(DayOfWeek.FRIDAY, new DailyWish(false, LocalTime.of(7, 30), LocalTime.of(14, 30)));
      SavedInput saved =
          savedOf(
              List.of(new EmployeeProfile("佐藤", EmploymentType.FULL_TIME, shifts)),
              List.of(),
              Optional.empty());

      List<DayForm> days = converter.toForm(saved, DEFAULT_MONTH).getEmployees().get(0).getDays();

      assertEquals("08:00", days.get(0).getStart());
      assertEquals("17:00", days.get(0).getEnd());
      assertEquals("09:00", days.get(1).getStart());
      assertEquals("16:00", days.get(1).getEnd());
      assertEquals("07:30", days.get(2).getStart());
      assertEquals("18:30", days.get(2).getEnd());
      assertEquals("07:30", days.get(3).getStart());
      assertEquals("18:30", days.get(3).getEnd());
      assertEquals("07:30", days.get(4).getStart());
      assertEquals("14:30", days.get(4).getEnd());
    }

    @Test
    @DisplayName("[F-7][8.1節] Given: 全曜日が休みの従業員が保存済み, When: フォームに変換すると, Then: 全曜日が 07:30〜18:30 になる")
    void restoresAllOffDaysAsDefaultTimeRange() {
      Map<DayOfWeek, DailyWish> shifts = new EnumMap<>(DayOfWeek.class);
      for (DayOfWeek day : DayOfWeek.values()) {
        shifts.put(day, new DailyWish(true, null, null));
      }
      SavedInput saved =
          savedOf(
              List.of(new EmployeeProfile("佐藤", EmploymentType.FULL_TIME, shifts)),
              List.of(),
              Optional.empty());

      List<DayForm> days = converter.toForm(saved, DEFAULT_MONTH).getEmployees().get(0).getDays();

      assertEquals(5, days.size());
      for (DayForm day : days) {
        assertEquals("07:30", day.getStart());
        assertEquals("18:30", day.getEnd());
      }
    }

    @Test
    @DisplayName(
        "[F-7][8.1節] Given: 休みだった曜日を含む旧仕様の基本シフトが保存済み, When: フォームに変換すると,"
            + " Then: 休みだった曜日は既定の 07:30〜18:30 になり、他の曜日は開始・終了が HH:mm で復元される")
    void restoresDaysWithTimeFormat() {
      SavedInput saved =
          savedOf(List.of(profile("佐藤", EmploymentType.FULL_TIME)), List.of(), Optional.empty());

      List<DayForm> days = converter.toForm(saved, DEFAULT_MONTH).getEmployees().get(0).getDays();

      assertEquals(5, days.size());
      assertEquals("07:30", days.get(0).getStart());
      assertEquals("18:30", days.get(0).getEnd());
      assertEquals("08:00", days.get(1).getStart());
      assertEquals("17:30", days.get(1).getEnd());
      assertEquals("09:00", days.get(2).getStart());
      assertEquals("18:00", days.get(2).getEnd());
      assertEquals("07:30", days.get(3).getStart());
      assertEquals("14:30", days.get(3).getEnd());
      assertEquals("10:00", days.get(4).getStart());
      assertEquals("18:30", days.get(4).getEnd());
    }

    @Test
    @DisplayName("[F-7][8.1節] Given: 復元した時刻, When: 選択肢と比べると, Then: すべて TimeOptions の値に含まれる")
    void restoredTimesAreInTimeOptions() {
      SavedInput saved =
          savedOf(List.of(profile("佐藤", EmploymentType.FULL_TIME)), List.of(), Optional.empty());

      List<DayForm> days = converter.toForm(saved, DEFAULT_MONTH).getEmployees().get(0).getDays();

      for (DayForm day : days) {
        assertTrue(TimeOptions.VALUES.contains(day.getStart()));
        assertTrue(TimeOptions.VALUES.contains(day.getEnd()));
      }
    }

    @Test
    @DisplayName("[F-7][8.1節] Given: 何も保存していない, When: フォームに変換すると, Then: 空の 12 行と既定の対象月になる")
    void returnsTwelveEmptyRowsAndDefaultMonthWhenNothingSaved() {
      ShiftForm form =
          converter.toForm(savedOf(List.of(), List.of(), Optional.empty()), DEFAULT_MONTH);

      assertEquals("2026-09", form.getTargetMonth());
      assertEquals(12, form.getEmployees().size());
      form.getEmployees().forEach(row -> assertEmptyRow(row));
      assertTrue(form.getAdjustments().isEmpty());
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
