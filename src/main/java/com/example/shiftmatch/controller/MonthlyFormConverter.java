package com.example.shiftmatch.controller;

import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.EmploymentType;
import com.example.shiftmatch.domain.MonthlyShiftInput;
import com.example.shiftmatch.domain.ShiftAdjustment;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * フォーム入力を {@link MonthlyShiftInput} に変換します。
 */
@Component
public class MonthlyFormConverter {

  /**
   * {@link ShiftForm} を {@link MonthlyShiftInput} に変換します。
   *
   * <p>対象月が無効な形式、または従業員の情報が不完全な場合、対応する部分は {@code null}
   * に置き換えられます。検証は別途行われます。
   *
   * @param form 入力フォーム
   * @return 変換後の入力データ
   */
  public MonthlyShiftInput toInput(ShiftForm form) {
    YearMonth month = parseMonth(form.getTargetMonth());
    List<EmployeeProfile> employees = convertEmployees(form.getEmployees());
    List<ShiftAdjustment> adjustments = convertAdjustments(form.getAdjustments());

    return new MonthlyShiftInput(month, employees, adjustments);
  }

  /**
   * 対象月の文字列を {@link YearMonth} に変換します。
   *
   * <p>形式が {@code YYYY-MM} でない場合は {@code null} を返します。
   *
   * @param targetMonth 対象月の文字列
   * @return 変換後の {@link YearMonth}、または変換失敗時は {@code null}
   */
  private YearMonth parseMonth(String targetMonth) {
    return InputParsers.parseYearMonth(targetMonth).orElse(null);
  }

  /**
   * 従業員フォームのリストを {@link EmployeeProfile} のリストに変換します。
   *
   * @param employeeForms 従業員フォームのリスト
   * @return 変換後の従業員プロファイルのリスト
   */
  private List<EmployeeProfile> convertEmployees(List<EmployeeForm> employeeForms) {
    List<EmployeeProfile> employees = new ArrayList<>();
    if (employeeForms == null) {
      return employees;
    }

    for (EmployeeForm form : employeeForms) {
      EmployeeProfile profile = convertEmployee(form);
      employees.add(profile);
    }
    return employees;
  }

  /**
   * 単一の従業員フォームを {@link EmployeeProfile} に変換します。
   *
   * @param form 従業員フォーム
   * @return 変換後の従業員プロファイル
   */
  private EmployeeProfile convertEmployee(EmployeeForm form) {
    String name = form.getName();
    Optional<EmploymentType> employmentType = EmploymentType.parse(form.getEmploymentType());
    Map<DayOfWeek, DailyWish> baseShifts = convertDays(form.getDays());
    Set<DayOfWeek> offDays = convertOffDays(form.getOffDays());

    return new EmployeeProfile(name, employmentType.orElse(null), baseShifts, offDays);
  }

  private static final DayOfWeek[] WEEKDAYS = {
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
  };

  /**
   * 曜日フォームのリストを基本シフトのマップに変換します。
   *
   * <p>{@code dayForms} の d 番目（0＝月）が存在し {@code null} でない曜日だけをマップに含めます。5 件より多い分は無視します。
   *
   * @param dayForms 曜日フォームのリスト
   * @return 基本シフトのマップ（キー: {@link DayOfWeek}、値: {@link DailyWish}）
   */
  private Map<DayOfWeek, DailyWish> convertDays(List<DayForm> dayForms) {
    Map<DayOfWeek, DailyWish> baseShifts = new EnumMap<>(DayOfWeek.class);
    if (dayForms == null) {
      return baseShifts;
    }
    for (int i = 0; i < WEEKDAYS.length; i++) {
      if (i < dayForms.size() && dayForms.get(i) != null) {
        baseShifts.put(WEEKDAYS[i], convertDay(dayForms.get(i)));
      }
    }
    return baseShifts;
  }

  /**
   * 単一の曜日フォームを {@link DailyWish} に変換します。
   *
   * @param form 曜日フォーム
   * @return 変換後の希望
   */
  private DailyWish convertDay(DayForm form) {
    LocalTime start = parseTime(form.getStart());
    LocalTime end = parseTime(form.getEnd());
    return new DailyWish(false, start, end);
  }

  /**
   * 個別変更フォームを {@link DailyWish} に変換します。
   *
   * <p>このメソッドは {@link AdjustmentForm} の `off`、`start`、`end` フィールドから
   * 希望を構築します。
   *
   * @param form 個別変更フォーム
   * @return 変換後の希望
   */
  private DailyWish convertDay(AdjustmentForm form) {
    if (form.isOff()) {
      return new DailyWish(true, null, null);
    }

    LocalTime start = parseTime(form.getStart());
    LocalTime end = parseTime(form.getEnd());
    return new DailyWish(false, start, end);
  }

  /**
   * 曜日休みの値（0＝月〜4＝金）を {@link DayOfWeek} の集合に変換します。
   *
   * <p>範囲外の値と {@code null} は無視します。
   *
   * @param values 曜日休みの値のリスト
   * @return 曜日休みの集合
   */
  private Set<DayOfWeek> convertOffDays(List<Integer> values) {
    Set<DayOfWeek> offDays = EnumSet.noneOf(DayOfWeek.class);
    if (values == null) {
      return offDays;
    }
    for (Integer value : values) {
      if (value != null && value >= 0 && value <= 4) {
        offDays.add(DayOfWeek.of(value + 1));
      }
    }
    return offDays;
  }

  /**
   * 時刻の文字列を {@link LocalTime} に変換します。
   *
   * <p>形式が {@code HH:mm} でない場合は {@code null} を返します。
   *
   * @param time 時刻の文字列
   * @return 変換後の {@link LocalTime}、または変換失敗時は {@code null}
   */
  private LocalTime parseTime(String time) {
    return InputParsers.parseTime(time).orElse(null);
  }

  /**
   * 日付の文字列を {@link LocalDate} に変換します。
   *
   * <p>形式が {@code YYYY-MM-DD} でない場合は {@link LocalDate#MIN} を返します。
   *
   * @param date 日付の文字列
   * @return 変換後の {@link LocalDate}、または変換失敗時は {@code LocalDate.MIN}
   */
  private LocalDate parseDate(String date) {
    return InputParsers.parseDate(date).orElse(LocalDate.MIN);
  }

  /**
   * 個別変更フォームのリストを {@link ShiftAdjustment} のリストに変換します。
   *
   * @param adjustmentForms 個別変更フォームのリスト
   * @return 変換後の個別変更のリスト
   */
  private List<ShiftAdjustment> convertAdjustments(List<AdjustmentForm> adjustmentForms) {
    List<ShiftAdjustment> adjustments = new ArrayList<>();
    if (adjustmentForms == null) {
      return adjustments;
    }

    for (AdjustmentForm form : adjustmentForms) {
      ShiftAdjustment adjustment = convertAdjustment(form);
      adjustments.add(adjustment);
    }
    return adjustments;
  }

  /**
   * 単一の個別変更フォームを {@link ShiftAdjustment} に変換します。
   *
   * @param form 個別変更フォーム
   * @return 変換後の個別変更
   */
  private ShiftAdjustment convertAdjustment(AdjustmentForm form) {
    LocalDate date = parseDate(form.getDate());
    String employeeName = form.getEmployeeName();
    if (employeeName == null) {
      employeeName = "";
    }

    DailyWish wish = convertDay(form);

    return new ShiftAdjustment(date, employeeName, wish);
  }
}
