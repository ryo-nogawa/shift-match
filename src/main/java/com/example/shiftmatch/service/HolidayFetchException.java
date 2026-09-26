package com.example.shiftmatch.service;

/**
 * 祝日 CSV 取得に失敗したことを表す例外。
 *
 * <p>ネットワークエラー、タイムアウト、HTTP エラーなど、取得に失敗した原因を保持します。
 */
public class HolidayFetchException extends RuntimeException {

  /**
   * メッセージを指定して例外を生成します。
   *
   * @param message エラーメッセージ
   */
  public HolidayFetchException(String message) {
    super(message);
  }

  /**
   * メッセージと原因を指定して例外を生成します。
   *
   * @param message エラーメッセージ
   * @param cause 原因の例外
   */
  public HolidayFetchException(String message, Throwable cause) {
    super(message, cause);
  }
}
