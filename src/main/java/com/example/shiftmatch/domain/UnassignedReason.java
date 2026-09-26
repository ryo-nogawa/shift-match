package com.example.shiftmatch.domain;

/**
 * 未出勤の理由を表す列挙型。
 */
public enum UnassignedReason {
  ON_LEAVE("休み"),
  LOWER_GAP_CHOSEN("入れる枠はあったが、より小さいずれの案が選ばれた"),
  NO_AVAILABLE_SLOT("どの枠にも入れない");

  private final String label;

  UnassignedReason(String label) {
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
