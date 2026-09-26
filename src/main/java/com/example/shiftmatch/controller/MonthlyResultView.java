package com.example.shiftmatch.controller;

import java.time.LocalDate;
import java.util.List;

/**
 * 結果画面（画面 3）のカレンダー表示と従業員別表示に使う表示モデルです。
 *
 * @param businessDayCount 営業日数
 * @param successCount 成立した日数
 * @param failureCount 不成立の日数
 * @param days 営業日ごとのカレンダー用の表示（営業日順）
 * @param holidayCells カレンダーに表示する祝日（月〜金のものだけ）
 * @param employeeRows 従業員別表示の行（入力順）
 */
public record MonthlyResultView(
    int businessDayCount,
    int successCount,
    int failureCount,
    List<CalendarDay> days,
    List<HolidayCell> holidayCells,
    List<EmployeeRow> employeeRows) {

  /**
   * カレンダー表示の営業日 1 日分です。
   *
   * @param date 日付
   * @param holidayName 祝日名（営業日には通常ありません。ない場合は {@code null}）
   * @param failed 不成立の日なら true
   * @param availableCount 勤務できる人数
   * @param groups 勤務時間ごとの氏名のまとまり（枠 1 → 6 の順）
   */
  public record CalendarDay(
      LocalDate date,
      String holidayName,
      boolean failed,
      int availableCount,
      List<WorkGroup> groups) {}

  /**
   * 同じ勤務時間の従業員のまとまりです。
   *
   * @param workTime 勤務時間（例：{@code 7:30–14:30}）
   * @param names 氏名を {@code ・} でつないだ文字列
   */
  public record WorkGroup(String workTime, String names) {}

  /**
   * カレンダーに表示する祝日です。
   *
   * @param date 日付
   * @param name 祝日名
   */
  public record HolidayCell(LocalDate date, String name) {}

  /**
   * 従業員別表示の 1 行です。
   *
   * @param name 従業員名
   * @param cells 営業日ごとのセル（勤務時間・{@code 休}・{@code –}・{@code ×}）
   * @param workDays 出勤日数
   */
  public record EmployeeRow(String name, List<String> cells, int workDays) {}
}
