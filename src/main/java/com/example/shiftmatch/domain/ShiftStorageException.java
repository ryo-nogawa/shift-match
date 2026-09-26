package com.example.shiftmatch.domain;

/**
 * シフトの保存に失敗したことを表す例外。
 *
 * <p>データベースへの保存中に発生したエラーを包みます。
 */
public class ShiftStorageException extends RuntimeException {

  /**
   * メッセージと原因を指定して例外を生成します。
   *
   * @param message エラーメッセージ
   * @param cause 原因の例外
   */
  public ShiftStorageException(String message, Throwable cause) {
    super(message, cause);
  }
}
