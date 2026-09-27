package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.ShiftAdjustment;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("[F-11] 希望の優先順位")
class WishResolverTest {

  private static final LocalTime DEFAULT_START = LocalTime.of(7, 30);
  private static final LocalTime DEFAULT_END = LocalTime.of(18, 30);
  private static final LocalDate MONDAY = LocalDate.of(2024, 9, 2);
  private static final LocalDate TUESDAY = LocalDate.of(2024, 9, 3);

  private static EmployeeProfile profile(EmploymentType type, Set<DayOfWeek> offDays) {
    return new EmployeeProfile("Taro", type, offDays);
  }

  @Nested
  class 正常系 {

    @Test
    @DisplayName("[F-11] Given: 個別変更も曜日休みもないとき, When: resolve を実行すると, Then: 7:30〜18:30 で出勤になる")
    void defaultWishWhenNothingSpecified() {
      EmployeeProfile profile = profile(EmploymentType.FULL_TIME, Set.of());

      DailyWish resolved = new WishResolver().resolve(profile, TUESDAY, List.of());

      assertFalse(resolved.off());
      assertEquals(DEFAULT_START, resolved.start());
      assertEquals(DEFAULT_END, resolved.end());
    }

    @Test
    @DisplayName("[F-1] Given: パートの曜日休み, When: 曜日休みの日を resolve すると, Then: 休みになる")
    void partTimeOffDayIsOff() {
      EmployeeProfile profile = profile(EmploymentType.PART_TIME, Set.of(DayOfWeek.TUESDAY));

      DailyWish resolved = new WishResolver().resolve(profile, TUESDAY, List.of());

      assertTrue(resolved.off());
    }

    @Test
    @DisplayName("[F-1] Given: パートの曜日休み, When: 曜日休みでない日を resolve すると, Then: 7:30〜18:30 で出勤になる")
    void partTimeNonOffDayIsDefault() {
      EmployeeProfile profile = profile(EmploymentType.PART_TIME, Set.of(DayOfWeek.TUESDAY));

      DailyWish resolved = new WishResolver().resolve(profile, MONDAY, List.of());

      assertFalse(resolved.off());
      assertEquals(DEFAULT_START, resolved.start());
      assertEquals(DEFAULT_END, resolved.end());
    }

    @Test
    @DisplayName("[F-1] Given: 常勤に曜日休みを渡したとき, When: resolve を実行すると, Then: 曜日休みは無視される")
    void fullTimeIgnoresOffDays() {
      EmployeeProfile profile = profile(EmploymentType.FULL_TIME, Set.of(DayOfWeek.TUESDAY));

      DailyWish resolved = new WishResolver().resolve(profile, TUESDAY, List.of());

      assertFalse(resolved.off());
      assertEquals(DEFAULT_START, resolved.start());
      assertEquals(DEFAULT_END, resolved.end());
    }

    @Test
    @DisplayName("[F-11] Given: 曜日休みの日に出勤の個別変更, When: resolve を実行すると, Then: 個別変更が優先される")
    void adjustmentOverridesOffDay() {
      EmployeeProfile profile = profile(EmploymentType.PART_TIME, Set.of(DayOfWeek.TUESDAY));
      DailyWish wish = new DailyWish(false, LocalTime.of(10, 0), LocalTime.of(17, 0));
      List<ShiftAdjustment> adjustments = List.of(new ShiftAdjustment(TUESDAY, "Taro", wish));

      DailyWish resolved = new WishResolver().resolve(profile, TUESDAY, adjustments);

      assertFalse(resolved.off());
      assertEquals(LocalTime.of(10, 0), resolved.start());
      assertEquals(LocalTime.of(17, 0), resolved.end());
    }

    @Test
    @DisplayName("[F-11] Given: 個別変更の日付が異なるとき, When: resolve を実行すると, Then: 7:30〜18:30 が使われる")
    void adjustmentSpecificToDate() {
      EmployeeProfile profile = profile(EmploymentType.FULL_TIME, Set.of());
      DailyWish wish = new DailyWish(false, LocalTime.of(10, 0), LocalTime.of(17, 0));
      List<ShiftAdjustment> adjustments = List.of(new ShiftAdjustment(MONDAY, "Taro", wish));

      DailyWish resolved = new WishResolver().resolve(profile, TUESDAY, adjustments);

      assertEquals(DEFAULT_START, resolved.start());
      assertEquals(DEFAULT_END, resolved.end());
    }

    @Test
    @DisplayName("[F-11] Given: 個別変更の氏名が一致しないとき, When: resolve を実行すると, Then: 個別変更は無視される")
    void adjustmentWithMismatchedName() {
      EmployeeProfile profile = profile(EmploymentType.FULL_TIME, Set.of());
      DailyWish wish = new DailyWish(false, LocalTime.of(10, 0), LocalTime.of(17, 0));
      List<ShiftAdjustment> adjustments = List.of(new ShiftAdjustment(TUESDAY, "Hanako", wish));

      DailyWish resolved = new WishResolver().resolve(profile, TUESDAY, adjustments);

      assertEquals(DEFAULT_START, resolved.start());
      assertEquals(DEFAULT_END, resolved.end());
    }

    @Test
    @DisplayName("[F-11] Given: 個別変更で休みへの変更があるとき, When: resolve を実行すると, Then: 休みが反映される")
    void adjustmentToLeave() {
      EmployeeProfile profile = profile(EmploymentType.FULL_TIME, Set.of());
      List<ShiftAdjustment> adjustments =
          List.of(new ShiftAdjustment(TUESDAY, "Taro", new DailyWish(true, null, null)));

      DailyWish resolved = new WishResolver().resolve(profile, TUESDAY, adjustments);

      assertTrue(resolved.off());
    }

    @Test
    @DisplayName("[F-11] Given: 同じ日付・氏名の個別変更が複数あるとき, When: resolve を実行すると, Then: 後ろのものを採用する")
    void lastAdjustmentWins() {
      EmployeeProfile profile = profile(EmploymentType.FULL_TIME, Set.of());
      DailyWish first = new DailyWish(false, LocalTime.of(10, 0), LocalTime.of(17, 0));
      DailyWish second = new DailyWish(false, LocalTime.of(11, 0), LocalTime.of(16, 0));
      List<ShiftAdjustment> adjustments =
          List.of(
              new ShiftAdjustment(TUESDAY, "Taro", first),
              new ShiftAdjustment(TUESDAY, "Taro", second));

      DailyWish resolved = new WishResolver().resolve(profile, TUESDAY, adjustments);

      assertEquals(LocalTime.of(11, 0), resolved.start());
      assertEquals(LocalTime.of(16, 0), resolved.end());
    }
  }
}
