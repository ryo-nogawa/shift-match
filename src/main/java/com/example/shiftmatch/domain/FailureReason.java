package com.example.shiftmatch.domain;

/**
 * 不成立の理由を表す列挙型。
 *
 * <p>人員不足とパートの週上限の 2 種類を区別します（6 章）。
 */
public enum FailureReason {
  /** 人員不足。その日の H-1〜H-3 を満たす案が 1 つもない。 */
  SHORTAGE("人員不足"),
  /** パートの週上限。その日の案はあるが、H-4 を守る週の最良案では成立させない。 */
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
