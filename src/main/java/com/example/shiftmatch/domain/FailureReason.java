package com.example.shiftmatch.domain;

/**
 * 不成立の理由を表す列挙型（6 章）。
 */
public enum FailureReason {
  /** 人員不足：H-4 を除いた H-1〜H-3 を満たす案も 1 つもない。 */
  STAFF_SHORTAGE("人員不足"),
  /** パートの週上限：H-1〜H-3 を満たす案はあるが、H-4 を加えると 1 つもない。 */
  WEEKLY_LIMIT("パートの週上限");

  private final String label;

  FailureReason(String label) {
    this.label = label;
  }

  /**
   * この理由の表示文言を返します。
   *
   * @return 表示文言
   */
  public String label() {
    return label;
  }
}
