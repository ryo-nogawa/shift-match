package com.example.shiftmatch.service;

import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.InputError;
import com.example.shiftmatch.domain.MonthlyShiftInput;
import com.example.shiftmatch.domain.ShiftAdjustment;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 月間シフト入力の検証を行います。
 *
 * <p>V-1 から V-9 の入力チェックを実行します。
 */
@Component
public class MonthlyInputValidator {

  private final HolidayService holidayService;

  /**
   * 月間入力検証を初期化します。
   *
   * @param holidayService 祝日サービス
   */
  public MonthlyInputValidator(HolidayService holidayService) {
    this.holidayService = holidayService;
  }

  /**
   * 月間シフト入力を検証し、エラーのリストを返します。
   *
   * <p>エラーは V-1 から V-9 の順に評価され、検出順に返されます。
   *
   * @param input 検証対象の入力
   * @return エラーのリスト（エラーがなければ空）
   */
  public List<InputError> validate(MonthlyShiftInput input) {
    List<InputError> errors = new ArrayList<>();

    // 有効な従業員（名前が空でない）のリストを作成
    List<EmployeeProfile> validEmployees = getValidEmployees(input.employees());

    // V-2: 従業員名の重複チェック
    List<InputError> v2Errors = validateDuplicateNames(validEmployees);
    errors.addAll(v2Errors);

    // V-3: 基本シフトと個別変更の時間帯チェック
    List<InputError> v3Errors = validateTimeRanges(validEmployees, input);
    errors.addAll(v3Errors);

    // V-5: 従業員数の上限チェック
    List<InputError> v5Errors = validateEmployeeCount(validEmployees);
    errors.addAll(v5Errors);

    // V-6: 従業員名の長さチェック
    List<InputError> v6Errors = validateNameLength(validEmployees);
    errors.addAll(v6Errors);

    // V-7: 雇用区分の検証
    List<InputError> v7Errors = validateEmploymentType(validEmployees);
    errors.addAll(v7Errors);

    // V-8: 対象月の判定可能性チェック
    List<InputError> v8Errors = validateMonthSupport(input.month());
    errors.addAll(v8Errors);

    // V-8 にエラーがなければ V-9 を実行
    if (v8Errors.isEmpty()) {
      List<InputError> v9Errors = validateAdjustmentDates(validEmployees, input);
      errors.addAll(v9Errors);
    }

    return errors;
  }

  private List<EmployeeProfile> getValidEmployees(List<EmployeeProfile> employees) {
    List<EmployeeProfile> valid = new ArrayList<>();
    for (EmployeeProfile profile : employees) {
      if (profile.name() != null && !profile.name().isBlank()) {
        valid.add(profile);
      }
    }
    return valid;
  }

  private List<InputError> validateDuplicateNames(List<EmployeeProfile> validEmployees) {
    List<InputError> errors = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    Set<String> duplicates = new HashSet<>();

    for (EmployeeProfile profile : validEmployees) {
      if (seen.contains(profile.name())) {
        duplicates.add(profile.name());
      }
      seen.add(profile.name());
    }

    if (!duplicates.isEmpty()) {
      String message = "従業員名が重複しています: " + String.join(", ", duplicates);
      errors.add(new InputError("V-2", message));
    }

    return errors;
  }

  private List<InputError> validateTimeRanges(
      List<EmployeeProfile> validEmployees, MonthlyShiftInput input) {
    List<InputError> errors = new ArrayList<>();

    // 基本シフトの検証
    for (int i = 0; i < validEmployees.size(); i++) {
      EmployeeProfile profile = validEmployees.get(i);
      List<InputError> profileErrors = validateBaseShifts(profile, i + 1);
      errors.addAll(profileErrors);
    }

    // 有効な従業員名の集合を作成
    Set<String> validNames = new HashSet<>();
    for (EmployeeProfile profile : validEmployees) {
      validNames.add(profile.name());
    }

    // 個別変更の時間帯検証
    List<InputError> adjustmentErrors =
        validateAdjustmentTimeRanges(validNames, input.adjustments());
    errors.addAll(adjustmentErrors);

    return errors;
  }

  private List<InputError> validateBaseShifts(EmployeeProfile profile, int lineNumber) {
    List<InputError> errors = new ArrayList<>();

    // 月〜金の曜日をチェック
    for (DayOfWeek day :
        new DayOfWeek[] {
          DayOfWeek.MONDAY,
          DayOfWeek.TUESDAY,
          DayOfWeek.WEDNESDAY,
          DayOfWeek.THURSDAY,
          DayOfWeek.FRIDAY
        }) {
      DailyWish wish = profile.baseShifts().get(day);

      if (wish == null) {
        // 曜日が欠けている
        String dayName = getDayName(day);
        String message = String.format("基本シフト：%s が未選択です（%d 行目）", dayName, lineNumber);
        errors.add(new InputError("V-3", message));
      } else if (!wish.off()) {
        // 時間帯の妥当性をチェック
        List<InputError> timeErrors = validateTimeRange(wish, day, profile.name(), lineNumber);
        errors.addAll(timeErrors);
      }
    }

    return errors;
  }

  private List<InputError> validateTimeRange(
      DailyWish wish, DayOfWeek day, String name, int lineNumber) {
    List<InputError> errors = new ArrayList<>();

    if (wish.start() == null || wish.end() == null) {
      String dayName = getDayName(day);
      String message = String.format("基本シフト：%s が未選択です（%s、%d 行目）", dayName, name, lineNumber);
      errors.add(new InputError("V-3", message));
    } else if (!isValidTime(wish.start()) || !isValidTime(wish.end())) {
      String dayName = getDayName(day);
      String message =
          String.format(
              "基本シフト：%s の時間帯が 7:30〜18:30 の 30 分単位ではありません（%s、%d 行目）", dayName, name, lineNumber);
      errors.add(new InputError("V-3", message));
    } else if (!wish.start().isBefore(wish.end())) {
      String dayName = getDayName(day);
      String message =
          String.format("基本シフト：%s の開始時刻が終了時刻以上です（%s、%d 行目）", dayName, name, lineNumber);
      errors.add(new InputError("V-3", message));
    }

    return errors;
  }

  private boolean isValidTime(LocalTime time) {
    LocalTime minTime = LocalTime.of(7, 30);
    LocalTime maxTime = LocalTime.of(18, 30);

    if (time.isBefore(minTime) || time.isAfter(maxTime)) {
      return false;
    }

    // 30 分単位かチェック
    return time.getMinute() == 0 || time.getMinute() == 30;
  }

  private String getDayName(DayOfWeek day) {
    return switch (day) {
      case MONDAY -> "月曜日";
      case TUESDAY -> "火曜日";
      case WEDNESDAY -> "水曜日";
      case THURSDAY -> "木曜日";
      case FRIDAY -> "金曜日";
      default -> day.toString();
    };
  }

  private List<InputError> validateEmployeeCount(List<EmployeeProfile> validEmployees) {
    List<InputError> errors = new ArrayList<>();
    if (validEmployees.size() >= 13) {
      String message = "有効な従業員が 13 名以上です";
      errors.add(new InputError("V-5", message));
    }
    return errors;
  }

  private List<InputError> validateNameLength(List<EmployeeProfile> validEmployees) {
    List<InputError> errors = new ArrayList<>();
    for (int i = 0; i < validEmployees.size(); i++) {
      EmployeeProfile profile = validEmployees.get(i);
      if (profile.name().length() > 255) {
        String message = String.format("従業員名が 255 文字を超えています（%d 行目）", i + 1);
        errors.add(new InputError("V-6", message));
      }
    }
    return errors;
  }

  private List<InputError> validateEmploymentType(List<EmployeeProfile> validEmployees) {
    List<InputError> errors = new ArrayList<>();
    for (int i = 0; i < validEmployees.size(); i++) {
      EmployeeProfile profile = validEmployees.get(i);
      if (profile.employmentType() == null
          || (profile.employmentType() != EmploymentType.FULL_TIME
              && profile.employmentType() != EmploymentType.PART_TIME
              && profile.employmentType() != EmploymentType.MANAGER)) {
        String message = String.format("雇用区分が不正です（%d 行目）", i + 1);
        errors.add(new InputError("V-7", message));
      }
    }
    return errors;
  }

  private List<InputError> validateMonthSupport(YearMonth month) {
    List<InputError> errors = new ArrayList<>();
    if (month == null) {
      String message = "対象月が指定されていません";
      errors.add(new InputError("V-8", message));
    } else if (!holidayService.isSupported(month)) {
      String message = String.format("対象月 %s の祝日データが利用できません", month);
      errors.add(new InputError("V-8", message));
    }
    return errors;
  }

  private List<InputError> validateAdjustmentTimeRanges(
      Set<String> validNames, List<ShiftAdjustment> adjustments) {
    List<InputError> errors = new ArrayList<>();

    for (ShiftAdjustment adjustment : adjustments) {
      // 従業員名が有効な従業員に一致する場合のみ検証
      if (validNames.contains(adjustment.employeeName())) {
        DailyWish wish = adjustment.wish();

        if (!wish.off()) {
          // 「休み」以外は時間帯を検証
          List<InputError> timeErrors = validateAdjustmentTimeRange(wish, adjustment);
          errors.addAll(timeErrors);
        }
      }
    }

    return errors;
  }

  private List<InputError> validateAdjustmentTimeRange(DailyWish wish, ShiftAdjustment adjustment) {
    List<InputError> errors = new ArrayList<>();

    if (wish.start() == null || wish.end() == null) {
      String message =
          String.format("個別変更の時間帯が未選択です（%s、%s）", adjustment.employeeName(), adjustment.date());
      errors.add(new InputError("V-3", message));
    } else if (!isValidTime(wish.start()) || !isValidTime(wish.end())) {
      String message =
          String.format(
              "個別変更の時間帯が 7:30〜18:30 の 30 分単位ではありません（%s、%s）",
              adjustment.employeeName(), adjustment.date());
      errors.add(new InputError("V-3", message));
    } else if (!wish.start().isBefore(wish.end())) {
      String message =
          String.format("個別変更の開始時刻が終了時刻以上です（%s、%s）", adjustment.employeeName(), adjustment.date());
      errors.add(new InputError("V-3", message));
    }

    return errors;
  }

  private List<InputError> validateAdjustmentDates(
      List<EmployeeProfile> validEmployees, MonthlyShiftInput input) {
    List<InputError> errors = new ArrayList<>();
    Set<String> validNames = new HashSet<>();
    for (EmployeeProfile profile : validEmployees) {
      validNames.add(profile.name());
    }

    List<LocalDate> businessDays = holidayService.businessDays(input.month());
    Set<LocalDate> businessDaySet = new HashSet<>(businessDays);

    for (ShiftAdjustment adjustment : input.adjustments()) {
      if (validNames.contains(adjustment.employeeName())) {
        if (!businessDaySet.contains(adjustment.date())) {
          String message = String.format("個別変更の日付が対象月の営業日ではありません（%s）", adjustment.date());
          errors.add(new InputError("V-9", message));
        }
      }
    }

    return errors;
  }
}
