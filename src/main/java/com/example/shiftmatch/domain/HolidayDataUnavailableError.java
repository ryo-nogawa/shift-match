package com.example.shiftmatch.domain;

import java.time.YearMonth;

/**
 * 祝日データが利用できないことを表すエラー。
 *
 * <p>指定された年月の祝日データが保存されていない場合に投げられます。
 */
public class HolidayDataUnavailableError extends RuntimeException {

  /**
   * 利用できない年月を指定してエラーを生成します。
   *
   * @param month 祝日データが利用できない年月
   */
  public HolidayDataUnavailableError(YearMonth month) {
    super(month.getYear() + "年" + month.getMonthValue() + "月の祝日データが利用できません");
  }
}
