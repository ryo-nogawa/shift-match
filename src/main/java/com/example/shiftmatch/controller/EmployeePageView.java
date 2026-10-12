package com.example.shiftmatch.controller;

import com.example.shiftmatch.domain.EmploymentType;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 従業員の個別ページ（7.3 節）の表示モデルです。
 *
 * @param month 対象月
 * @param name 従業員名
 * @param employmentType 区分
 * @param weeks 週のリスト（対象月の 1 日を含む週から末日を含む週まで）
 * @param workDays 出勤日数
 * @param totalMinutes 月間の実労働時間の合計（分）
 * @param failureCount 対象月の不成立日数
 */
public record EmployeePageView(
    YearMonth month,
    String name,
    EmploymentType employmentType,
    List<Week> weeks,
    int workDays,
    int totalMinutes,
    int failureCount) {

  /**
   * 月間の合計の実労働時間を {@code (hh:mm)} 形式で返します。
   *
   * @return 例：{@code (14:45)}
   */
  public String durationLabel() {
    return durationLabelOf(totalMinutes);
  }

  private static String durationLabelOf(int minutes) {
    return String.format("(%02d:%02d)", minutes / 60, minutes % 60);
  }

  /**
   * カレンダーの 1 週（月曜〜日曜）です。
   *
   * @param days 月曜〜日曜の 7 マス
   * @param totalMinutes 週の実労働時間の合計（分。対象月の営業日だけ）
   */
  public record Week(List<DayCell> days, int totalMinutes) {

    /**
     * 週の合計の実労働時間を {@code (hh:mm)} 形式で返します。
     *
     * @return 例：{@code (07:30)}
     */
    public String durationLabel() {
      return durationLabelOf(totalMinutes);
    }
  }

  /**
   * カレンダーの 1 マスです。
   *
   * @param date 日付（対象月以外の空きのマスは {@code null}）
   * @param type マスの種類
   * @param text 表示文字列
   */
  public record DayCell(LocalDate date, CellType type, String text) {}

  /** マスの種類です。 */
  public enum CellType {
    /** 勤務あり。 */
    WORK,
    /** 休み。 */
    OFF,
    /** 割り当てなし。 */
    NOT_ASSIGNED,
    /** 不成立。 */
    FAILED,
    /** 祝日でない土日。 */
    WEEKEND,
    /** 祝日。 */
    HOLIDAY,
    /** 対象月以外の空き。 */
    BLANK
  }
}
