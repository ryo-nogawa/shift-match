package com.example.shiftmatch.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
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

  /**
   * カレンダーの 1 マスです。営業日・祝日・空きのいずれか 1 つだけが設定されます。
   *
   * @param day 営業日（営業日でなければ {@code null}）
   * @param holiday 祝日（祝日でなければ {@code null}）
   */
  public record CalendarCell(CalendarDay day, HolidayCell holiday) {

    /**
     * 月初の曜日をそろえるための空きのマスかどうかを返します。
     *
     * @return 空きのマスなら true
     */
    public boolean blank() {
      return day == null && holiday == null;
    }
  }

  /**
   * 月〜金の 5 列に並べるマスを、日付順で返します。
   *
   * <p>月初が月曜でない場合は、先頭に空きのマスを入れて曜日をそろえます。
   *
   * @return カレンダーのマス
   */
  public List<CalendarCell> calendarCells() {
    List<CalendarCell> cells = new ArrayList<>();
    int dayIndex = 0;
    int holidayIndex = 0;
    boolean first = true;
    while (dayIndex < days.size() || holidayIndex < holidayCells.size()) {
      boolean takeDay =
          holidayIndex >= holidayCells.size()
              || (dayIndex < days.size()
                  && days.get(dayIndex).date().isBefore(holidayCells.get(holidayIndex).date()));
      CalendarCell cell;
      LocalDate date;
      if (takeDay) {
        cell = new CalendarCell(days.get(dayIndex), null);
        date = days.get(dayIndex).date();
        dayIndex++;
      } else {
        cell = new CalendarCell(null, holidayCells.get(holidayIndex));
        date = holidayCells.get(holidayIndex).date();
        holidayIndex++;
      }
      if (first) {
        int offset = date.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
        for (int i = 0; i < offset; i++) {
          cells.add(new CalendarCell(null, null));
        }
        first = false;
      }
      cells.add(cell);
    }
    return cells;
  }
}
