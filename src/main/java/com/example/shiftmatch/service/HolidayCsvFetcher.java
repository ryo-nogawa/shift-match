package com.example.shiftmatch.service;

/**
 * 祝日 CSV を取得するインタフェース。
 *
 * <p>外部のデータソースから CSV をバイト列で取得します。
 */
public interface HolidayCsvFetcher {

  /**
   * 祝日 CSV をバイト列で取得します。
   *
   * @return Shift_JIS でエンコードされた CSV バイト列
   * @throws HolidayFetchException 取得に失敗した場合
   */
  byte[] fetch();
}
