package com.example.shiftmatch.domain;

import java.time.YearMonth;
import java.util.List;

/**
 * 月間シフト作成の結果を表すレコード。
 *
 * <p>営業日ごとのシフト算出結果を営業日順で保持します。
 */
public record MonthlyShiftResult(YearMonth month, List<DailyShiftResult> days) {

  /**
   * 日次結果リストを不変にする。
   *
   * <p>呼び出し側がリストを変更しても、レコードの内容に反映されないようにします。
   */
  public MonthlyShiftResult {
    days = List.copyOf(days);
  }
}
