package com.example.shiftmatch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.shiftmatch.domain.AssignmentResult;
import com.example.shiftmatch.domain.DailyShiftResult;
import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.Employee;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.FailureReason;
import com.example.shiftmatch.domain.InputError;
import com.example.shiftmatch.domain.InvalidMonthlyInputException;
import com.example.shiftmatch.domain.MonthlyShiftInput;
import com.example.shiftmatch.domain.MonthlyShiftResult;
import com.example.shiftmatch.domain.ShiftAdjustment;
import com.example.shiftmatch.domain.ShiftAssignment;
import com.example.shiftmatch.domain.UnassignedReason;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("[F-3] 営業日ごとの独立した割り当て算出")
class MonthlyShiftServiceImplTest {

  @Nested
  class 正常系 {

    @Test
    @DisplayName(
        "[F-3] Given: businessDays が返した日が複数あるとき, When: create を実行すると, Then: その日だけが結果に含まれ順序も同じ")
    void onlyBusinessDaysInResult() {
      HolidayService holidayService = mock(HolidayService.class);
      ShiftAssignmentService assignmentService = mock(ShiftAssignmentService.class);

      LocalDate day1 = LocalDate.of(2024, 9, 2); // Monday
      LocalDate day2 = LocalDate.of(2024, 9, 3); // Tuesday
      YearMonth month = YearMonth.of(2024, 9);

      when(holidayService.businessDays(month)).thenReturn(List.of(day1, day2));
      when(holidayService.isSupported(month)).thenReturn(true);

      MonthlyInputValidator inputValidator = mock(MonthlyInputValidator.class);
      SelectionRationaleLogger logger = mock(SelectionRationaleLogger.class);
      MonthlyShiftServiceImpl service =
          new MonthlyShiftServiceImpl(holidayService, assignmentService, inputValidator, logger);

      EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of());
      MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), new ArrayList<>());

      MonthlyShiftResult result = service.create(input);

      assertEquals(month, result.month());
      assertEquals(2, result.days().size());
      assertEquals(day1, result.days().get(0).date());
      assertEquals(day2, result.days().get(1).date());
    }

    @Test
    @DisplayName("[V-1] Given: 従業員名が空の行を含むとき, When: create を実行すると, Then: 空行は処理対象から除外される")
    void emptyNameExcluded() {
      HolidayService holidayService = mock(HolidayService.class);
      ShiftAssignmentService assignmentService = mock(ShiftAssignmentService.class);

      LocalDate day1 = LocalDate.of(2024, 9, 2);
      YearMonth month = YearMonth.of(2024, 9);

      when(holidayService.businessDays(month)).thenReturn(List.of(day1));
      when(holidayService.isSupported(month)).thenReturn(true);

      MonthlyInputValidator inputValidator = mock(MonthlyInputValidator.class);
      SelectionRationaleLogger logger = mock(SelectionRationaleLogger.class);
      MonthlyShiftServiceImpl service =
          new MonthlyShiftServiceImpl(holidayService, assignmentService, inputValidator, logger);

      EmployeeProfile taroProfile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of());
      EmployeeProfile emptyNameProfile =
          new EmployeeProfile("", EmploymentType.FULL_TIME, Set.of());

      MonthlyShiftInput input =
          new MonthlyShiftInput(month, List.of(taroProfile, emptyNameProfile), new ArrayList<>());

      MonthlyShiftResult result = service.create(input);

      // verify empty name is excluded
      assertEquals(1, result.days().size());
      DailyShiftResult dayResult = result.days().get(0);
      assertEquals(1, dayResult.availableCount());
    }
  }

  @Nested
  class V4異常系_利用可能人数チェック {

    @Test
    @DisplayName("[V-4] Given: 勤務できる人が 8 名未満のときの日があるとき, When: create を実行すると, Then: その日は割り当て結果がない")
    void unsuccessfulWhenFewerThan8Available() {
      HolidayService holidayService = mock(HolidayService.class);

      LocalDate day1 = LocalDate.of(2024, 9, 2); // 8 名可能
      LocalDate day2 = LocalDate.of(2024, 9, 3); // 7 名可能
      YearMonth month = YearMonth.of(2024, 9);

      when(holidayService.businessDays(month)).thenReturn(List.of(day1, day2));
      when(holidayService.isSupported(month)).thenReturn(true);

      // 8 人の従業員
      List<EmployeeProfile> employees = new ArrayList<>();
      for (int i = 0; i < 8; i++) {
        employees.add(new EmployeeProfile("Employee" + i, EmploymentType.FULL_TIME, Set.of()));
      }

      // 火曜日（day2）だけ従業員 0 が休み（7 名だけ勤務可能）
      LocalDate tuesdayDate = LocalDate.of(2024, 9, 3);
      DailyWish offWish = new DailyWish(true, null, null);
      ShiftAdjustment adjustment = new ShiftAdjustment(tuesdayDate, "Employee0", offWish);

      MonthlyShiftInput input = new MonthlyShiftInput(month, employees, List.of(adjustment));

      ShiftAssignmentService assignmentService = mock(ShiftAssignmentService.class);
      when(assignmentService.assign(anyList()))
          .thenReturn(createDummyAssignmentResult())
          .thenReturn(Optional.empty());

      MonthlyInputValidator inputValidator = mock(MonthlyInputValidator.class);
      SelectionRationaleLogger logger = mock(SelectionRationaleLogger.class);
      MonthlyShiftServiceImpl service =
          new MonthlyShiftServiceImpl(holidayService, assignmentService, inputValidator, logger);

      MonthlyShiftResult result = service.create(input);

      assertEquals(2, result.days().size());
      DailyShiftResult day1Result = result.days().get(0);
      DailyShiftResult day2Result = result.days().get(1);

      // 月曜日は 8 人が勤務可能
      assertEquals(8, day1Result.availableCount());
      assertTrue(day1Result.assignment().isPresent(), "day1 should have an assignment");

      // 火曜日は 7 人が勤務可能
      assertEquals(7, day2Result.availableCount());
      assertTrue(day2Result.assignment().isEmpty(), "day2 should not have an assignment");
      // 6 章：不成立の理由は人員不足（H-4 を除いても成立しないため）
      assertEquals(FailureReason.STAFF_SHORTAGE, day2Result.failureReason());

      // day1 で 1 回、day2 は不成立の理由判定のため H-4 を除いてもう一度呼ぶので 2 回、計 3 回
      verify(assignmentService, times(3)).assign(anyList());
    }
  }

  @Nested
  class V3V9異常系_検証エラー {

    @Test
    @DisplayName(
        "[V-3] Given: 個別変更の検証エラーがあるとき, When: create を実行すると, Then: InvalidMonthlyInputException を投げ"
            + " assign を呼ばない")
    void validationErrorThrowsException() {
      YearMonth month = YearMonth.of(2024, 9);
      LocalDate businessDay = LocalDate.of(2024, 9, 2);

      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(month)).thenReturn(true);
      when(holidayService.businessDays(month)).thenReturn(List.of(businessDay));

      EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of());
      MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), new ArrayList<>());

      ShiftAssignmentService assignmentService = mock(ShiftAssignmentService.class);
      MonthlyInputValidator inputValidator = mock(MonthlyInputValidator.class);
      // V-3 エラーを返すようにスタブを設定
      InputError error = new InputError("V-3", "個別変更：開始・終了が未選択です（Taro、2024-09-02）");
      when(inputValidator.validate(input)).thenReturn(List.of(error));

      SelectionRationaleLogger logger = mock(SelectionRationaleLogger.class);
      MonthlyShiftServiceImpl service =
          new MonthlyShiftServiceImpl(holidayService, assignmentService, inputValidator, logger);
      assertThrows(InvalidMonthlyInputException.class, () -> service.create(input));

      // verify assign was never called
      verifyNoInteractions(assignmentService);
    }

    @Test
    @DisplayName(
        "[V-9] Given: 個別変更検証エラーがあるとき, When: create を実行すると, Then: InvalidMonthlyInputException を投げ"
            + " assign を呼ばない")
    void v9ValidationErrorThrowsException() {
      YearMonth month = YearMonth.of(2024, 9);
      LocalDate businessDay = LocalDate.of(2024, 9, 2);
      LocalTime start = LocalTime.of(9, 0);
      LocalTime end = LocalTime.of(18, 0);
      DailyWish wish = new DailyWish(false, start, end);

      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.isSupported(month)).thenReturn(true);
      when(holidayService.businessDays(month)).thenReturn(List.of(businessDay));

      EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of());

      // 営業日外の日付で個別変更 - V-9 error
      ShiftAdjustment adjustment = new ShiftAdjustment(LocalDate.of(2024, 9, 7), "Taro", wish);
      MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), List.of(adjustment));

      ShiftAssignmentService assignmentService = mock(ShiftAssignmentService.class);
      MonthlyInputValidator inputValidator = mock(MonthlyInputValidator.class);
      // V-9 エラーを返すようにスタブを設定
      InputError error = new InputError("V-9", "個別変更の日付が対象月の営業日ではありません（2024-09-07）");
      when(inputValidator.validate(input)).thenReturn(List.of(error));

      SelectionRationaleLogger logger = mock(SelectionRationaleLogger.class);
      MonthlyShiftServiceImpl service =
          new MonthlyShiftServiceImpl(holidayService, assignmentService, inputValidator, logger);

      assertThrows(InvalidMonthlyInputException.class, () -> service.create(input));

      // verify assign was never called
      verifyNoInteractions(assignmentService);
    }
  }

  @Nested
  class その他 {

    @Test
    @DisplayName("[7.2 節] Given: 複数の営業日があるとき, When: create を実行すると, Then: 営業日ごとにログを出力する")
    void createLogsForEachBusinessDay() {
      LocalDate day1 = LocalDate.of(2024, 9, 2); // Monday
      LocalDate day2 = LocalDate.of(2024, 9, 3); // Tuesday
      YearMonth month = YearMonth.of(2024, 9);

      HolidayService holidayService = mock(HolidayService.class);
      when(holidayService.businessDays(month)).thenReturn(List.of(day1, day2));
      when(holidayService.isSupported(month)).thenReturn(true);

      ShiftAssignmentService assignmentService = mock(ShiftAssignmentService.class);
      when(assignmentService.assign(any())).thenReturn(java.util.Optional.empty());

      MonthlyInputValidator inputValidator = mock(MonthlyInputValidator.class);
      when(inputValidator.validate(any())).thenReturn(new ArrayList<>());

      SelectionRationaleLogger rationaleLogger = mock(SelectionRationaleLogger.class);

      EmployeeProfile profile = new EmployeeProfile("Taro", EmploymentType.FULL_TIME, Set.of());

      MonthlyShiftInput input = new MonthlyShiftInput(month, List.of(profile), List.of());

      MonthlyShiftServiceImpl service =
          new MonthlyShiftServiceImpl(
              holidayService, assignmentService, inputValidator, rationaleLogger);
      service.create(input);

      // verify logger.log was called for each business day
      verify(rationaleLogger).log(eq(day1), any());
      verify(rationaleLogger).log(eq(day2), any());
    }
  }

  @Nested
  @DisplayName("[H-4][5.5][6章] パートの週の実労働時間の引き継ぎ")
  class WeeklyPartTimeCarryOver {

    private MonthlyShiftServiceImpl serviceWithRealAssignment(
        HolidayService holidayService, List<LocalDate> businessDays, YearMonth month) {
      when(holidayService.businessDays(month)).thenReturn(businessDays);
      when(holidayService.isSupported(month)).thenReturn(true);

      ShiftAssignmentService assignmentService = new ShiftAssignmentServiceImpl();
      MonthlyInputValidator inputValidator = mock(MonthlyInputValidator.class);
      when(inputValidator.validate(any())).thenReturn(new ArrayList<>());
      SelectionRationaleLogger logger = mock(SelectionRationaleLogger.class);

      return new MonthlyShiftServiceImpl(holidayService, assignmentService, inputValidator, logger);
    }

    // 常勤 7 名（7:30〜18:30）＋パート 1 名（7:30〜14:30、枠1にしか入れない）の 8 名ちょうどの構成
    private List<EmployeeProfile> sevenFullTimeAndOnePartTime() {
      List<EmployeeProfile> profiles = new ArrayList<>();
      for (int i = 0; i < 7; i++) {
        profiles.add(new EmployeeProfile("Full" + i, EmploymentType.FULL_TIME, Set.of()));
      }
      profiles.add(
          new EmployeeProfile("Part0", EmploymentType.PART_TIME, partTimeShifts(), Set.of()));
      return profiles;
    }

    private Map<DayOfWeek, DailyWish> partTimeShifts() {
      Map<DayOfWeek, DailyWish> shifts = new EnumMap<>(DayOfWeek.class);
      DailyWish wish = new DailyWish(false, LocalTime.of(7, 30), LocalTime.of(14, 30));
      for (DayOfWeek day :
          new DayOfWeek[] {
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
          }) {
        shifts.put(day, wish);
      }
      return shifts;
    }

    @Test
    @DisplayName(
        "[H-4][5.5] Given: 月〜金の週で8名ちょうど（パート1名は枠1にしか入れない）のとき, When: createを実行すると, Then:"
            + " パートは月・火・水に割り当てられ、木・金は週上限で不成立になる")
    void partIsAssignedForThreeDaysThenWeeklyLimitOnRemainingDays() {
      YearMonth month = YearMonth.of(2024, 9);
      LocalDate mon = LocalDate.of(2024, 9, 2);
      LocalDate tue = LocalDate.of(2024, 9, 3);
      LocalDate wed = LocalDate.of(2024, 9, 4);
      LocalDate thu = LocalDate.of(2024, 9, 5);
      LocalDate fri = LocalDate.of(2024, 9, 6);
      List<LocalDate> businessDays = List.of(mon, tue, wed, thu, fri);

      HolidayService holidayService = mock(HolidayService.class);
      MonthlyShiftServiceImpl service =
          serviceWithRealAssignment(holidayService, businessDays, month);

      MonthlyShiftInput input =
          new MonthlyShiftInput(month, sevenFullTimeAndOnePartTime(), new ArrayList<>());

      MonthlyShiftResult result = service.create(input);

      List<DailyShiftResult> days = result.days();
      assertEquals(5, days.size());

      assertTrue(days.get(0).assignment().isPresent(), "月曜日は成立するはず");
      assertTrue(days.get(1).assignment().isPresent(), "火曜日は成立するはず");
      assertTrue(days.get(2).assignment().isPresent(), "水曜日は成立するはず");

      assertTrue(days.get(3).assignment().isEmpty(), "木曜日は不成立のはず");
      assertEquals(FailureReason.WEEKLY_LIMIT, days.get(3).failureReason());

      assertTrue(days.get(4).assignment().isEmpty(), "金曜日は不成立のはず");
      assertEquals(FailureReason.WEEKLY_LIMIT, days.get(4).failureReason());
    }

    @Test
    @DisplayName(
        "[H-4][5.5] Given: 週をまたいだ翌週の月曜日のとき, When: createを実行すると, Then: パートの残り時間が0に戻り再び割り当てられる")
    void resetsRemainingMinutesOnNextWeek() {
      YearMonth month = YearMonth.of(2024, 9);
      // 1 週目：月〜金（パートは木・金で不成立になる） + 2 週目：月曜日
      LocalDate mon1 = LocalDate.of(2024, 9, 2);
      LocalDate tue1 = LocalDate.of(2024, 9, 3);
      LocalDate wed1 = LocalDate.of(2024, 9, 4);
      LocalDate thu1 = LocalDate.of(2024, 9, 5);
      LocalDate fri1 = LocalDate.of(2024, 9, 6);
      LocalDate mon2 = LocalDate.of(2024, 9, 9);
      List<LocalDate> businessDays = List.of(mon1, tue1, wed1, thu1, fri1, mon2);

      HolidayService holidayService = mock(HolidayService.class);
      MonthlyShiftServiceImpl service =
          serviceWithRealAssignment(holidayService, businessDays, month);

      MonthlyShiftInput input =
          new MonthlyShiftInput(month, sevenFullTimeAndOnePartTime(), new ArrayList<>());

      MonthlyShiftResult result = service.create(input);

      List<DailyShiftResult> days = result.days();
      assertEquals(6, days.size());
      DailyShiftResult secondWeekMonday = days.get(5);

      assertTrue(secondWeekMonday.assignment().isPresent(), "翌週の月曜日は残り時間が0に戻り成立するはず");
      boolean partAssigned =
          secondWeekMonday.assignment().get().assignments().stream()
              .anyMatch(a -> a.employee().name().equals("Part0"));
      assertTrue(partAssigned, "パートが翌週の月曜日に割り当てられているはず");
    }

    @Test
    @DisplayName(
        "[H-4][5.5] Given: 対象月が木曜日から始まる月（2026年10月）のとき, When: createを実行すると, Then:"
            + " 最初の週は木・金だけで数え、翌週の月曜から数え直す")
    void countsFromThursdayInFirstPartialWeekThenRestartsNextWeek() {
      YearMonth month = YearMonth.of(2026, 10);
      LocalDate thu1 = LocalDate.of(2026, 10, 1);
      LocalDate fri1 = LocalDate.of(2026, 10, 2);
      LocalDate mon2 = LocalDate.of(2026, 10, 5);
      LocalDate tue2 = LocalDate.of(2026, 10, 6);
      LocalDate wed2 = LocalDate.of(2026, 10, 7);
      LocalDate thu2 = LocalDate.of(2026, 10, 8);
      List<LocalDate> businessDays = List.of(thu1, fri1, mon2, tue2, wed2, thu2);

      HolidayService holidayService = mock(HolidayService.class);
      MonthlyShiftServiceImpl service =
          serviceWithRealAssignment(holidayService, businessDays, month);

      MonthlyShiftInput input =
          new MonthlyShiftInput(month, sevenFullTimeAndOnePartTime(), new ArrayList<>());

      MonthlyShiftResult result = service.create(input);

      List<DailyShiftResult> days = result.days();
      assertEquals(6, days.size());

      assertTrue(days.get(0).assignment().isPresent(), "10/1（木）は成立するはず");
      assertTrue(days.get(1).assignment().isPresent(), "10/2（金）は成立するはず");
      assertTrue(days.get(2).assignment().isPresent(), "10/5（月）は成立するはず");
      assertTrue(days.get(3).assignment().isPresent(), "10/6（火）は成立するはず");
      assertTrue(days.get(4).assignment().isPresent(), "10/7（水）は成立するはず");

      assertTrue(days.get(5).assignment().isEmpty(), "10/8（木）は不成立のはず");
      assertEquals(FailureReason.WEEKLY_LIMIT, days.get(5).failureReason());
    }

    @Test
    @DisplayName(
        "[H-4] Given: 同じ構成でパートを常勤に変えたとき, When: createを実行すると, Then: 全営業日が成立する（常勤には上限を適用しない）")
    void allDaysSucceedWhenPartTimeReplacedWithFullTime() {
      YearMonth month = YearMonth.of(2024, 9);
      LocalDate mon = LocalDate.of(2024, 9, 2);
      LocalDate tue = LocalDate.of(2024, 9, 3);
      LocalDate wed = LocalDate.of(2024, 9, 4);
      LocalDate thu = LocalDate.of(2024, 9, 5);
      LocalDate fri = LocalDate.of(2024, 9, 6);
      List<LocalDate> businessDays = List.of(mon, tue, wed, thu, fri);

      HolidayService holidayService = mock(HolidayService.class);
      MonthlyShiftServiceImpl service =
          serviceWithRealAssignment(holidayService, businessDays, month);

      List<EmployeeProfile> profiles = new ArrayList<>();
      for (int i = 0; i < 7; i++) {
        profiles.add(new EmployeeProfile("Full" + i, EmploymentType.FULL_TIME, Set.of()));
      }
      // パートだった枠を常勤（時間帯は同じ 7:30〜14:30）に変更
      profiles.add(
          new EmployeeProfile("Full7", EmploymentType.FULL_TIME, partTimeShifts(), Set.of()));

      MonthlyShiftInput input = new MonthlyShiftInput(month, profiles, new ArrayList<>());

      MonthlyShiftResult result = service.create(input);

      for (DailyShiftResult day : result.days()) {
        assertTrue(day.assignment().isPresent(), day.date() + " は成立するはず");
      }
    }

    @Test
    @DisplayName(
        "[6章] Given: 個別変更で1名を休みにして勤務できる人が7名になった日のとき, When: createを実行すると, Then:"
            + " その日の理由がSTAFF_SHORTAGEになる")
    void staffShortageWhenOneEmployeeIsOffReducingBelowEight() {
      YearMonth month = YearMonth.of(2024, 9);
      LocalDate mon = LocalDate.of(2024, 9, 2);
      List<LocalDate> businessDays = List.of(mon);

      HolidayService holidayService = mock(HolidayService.class);
      MonthlyShiftServiceImpl service =
          serviceWithRealAssignment(holidayService, businessDays, month);

      ShiftAdjustment adjustment =
          new ShiftAdjustment(mon, "Full0", new DailyWish(true, null, null));
      MonthlyShiftInput input =
          new MonthlyShiftInput(month, sevenFullTimeAndOnePartTime(), List.of(adjustment));

      MonthlyShiftResult result = service.create(input);

      DailyShiftResult dayResult = result.days().get(0);
      assertTrue(dayResult.assignment().isEmpty(), "7名では不成立のはず");
      assertEquals(FailureReason.STAFF_SHORTAGE, dayResult.failureReason());
    }

    @Test
    @DisplayName(
        "[8.4][H-4] Given: 月・火に枠1（375分）へ割り当てられたパートを個別変更で水曜に休みにしたとき, When: createを実行すると, Then:"
            + " 水曜の未出勤者のそのパートのweeklyRemainingMinutesが450、unassignedReasonがON_LEAVEである")
    void offPartTimeKeepsWeeklyRemainingMinutesInUnassignedEmployee() {
      YearMonth month = YearMonth.of(2024, 9);
      LocalDate mon = LocalDate.of(2024, 9, 2);
      LocalDate tue = LocalDate.of(2024, 9, 3);
      LocalDate wed = LocalDate.of(2024, 9, 4);
      List<LocalDate> businessDays = List.of(mon, tue, wed);

      HolidayService holidayService = mock(HolidayService.class);
      MonthlyShiftServiceImpl service =
          serviceWithRealAssignment(holidayService, businessDays, month);

      List<EmployeeProfile> profiles = new ArrayList<>();
      for (int i = 0; i < 8; i++) {
        profiles.add(new EmployeeProfile("Full" + i, EmploymentType.FULL_TIME, Set.of()));
      }
      profiles.add(
          new EmployeeProfile("Part0", EmploymentType.PART_TIME, partTimeShifts(), Set.of()));

      ShiftAdjustment offOnWednesday =
          new ShiftAdjustment(wed, "Part0", new DailyWish(true, null, null));
      MonthlyShiftInput input = new MonthlyShiftInput(month, profiles, List.of(offOnWednesday));

      MonthlyShiftResult result = service.create(input);

      List<DailyShiftResult> days = result.days();
      assertEquals(3, days.size());
      DailyShiftResult wedResult = days.get(2);
      assertTrue(wedResult.assignment().isPresent(), "水曜日は成立するはず");

      Employee part0Unassigned =
          wedResult.assignment().get().unassignedEmployees().stream()
              .filter(e -> e.name().equals("Part0"))
              .findFirst()
              .orElseThrow();

      assertEquals(450, part0Unassigned.weeklyRemainingMinutes());
      assertEquals(UnassignedReason.ON_LEAVE, part0Unassigned.unassignedReason());
    }
  }

  private Optional<AssignmentResult> createDummyAssignmentResult() {
    List<ShiftAssignment> assignments = new ArrayList<>();
    com.example.shiftmatch.domain.ShiftSlot[] slots =
        com.example.shiftmatch.domain.ShiftSlot.values();
    for (int i = 0; i < 8; i++) {
      com.example.shiftmatch.domain.Employee employee =
          com.example.shiftmatch.domain.Employee.working(
              "Dummy" + i, LocalTime.of(7, 30), LocalTime.of(18, 30));
      ShiftAssignment assignment =
          new ShiftAssignment(
              employee,
              slots[Math.min(i, slots.length - 1)],
              LocalTime.of(12, 0),
              LocalTime.of(12, 45));
      assignments.add(assignment);
    }
    AssignmentResult result = new AssignmentResult(assignments, 0, List.of());
    return Optional.of(result);
  }
}
