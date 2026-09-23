package com.example.shiftmatch.domain;

/**
 * 早番・遅番希望の不正値エラーを表します。
 */
public class InvalidWishError {

  /** エラーが発生した行番号（0 始まり）。 */
  private final int rowIndex;

  /** エラーの対象（早番希望 または 遅番希望）。 */
  private final String wishLabel;

  /**
   * コンストラクタです。
   *
   * @param rowIndex エラーが発生した行番号（0 始まり）
   * @param wishLabel エラーの対象（早番希望 または 遅番希望）
   */
  public InvalidWishError(int rowIndex, String wishLabel) {
    this.rowIndex = rowIndex;
    this.wishLabel = wishLabel;
  }

  /**
   * エラーが発生した行番号を取得します。
   *
   * @return 行番号（0 始まり）
   */
  public int getRowIndex() {
    return rowIndex;
  }

  /**
   * エラーの対象を取得します。
   *
   * @return エラーの対象（早番希望 または 遅番希望）
   */
  public String getWishLabel() {
    return wishLabel;
  }
}
