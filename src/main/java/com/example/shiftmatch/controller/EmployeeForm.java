package com.example.shiftmatch.controller;

/**
 * フォームで送信される従業員の入力データを保持します。
 */
public class EmployeeForm {

  /** 従業員の名前。 */
  private String name;

  /** 早番の希望（Wish enum の定数名）。 */
  private String earlyWish;

  /** 遅番の希望（Wish enum の定数名）。 */
  private String lateWish;

  /** デフォルトコンストラクタ。 */
  public EmployeeForm() {}

  /**
   * 従業員の名前を取得します。
   *
   * @return 従業員の名前
   */
  public String getName() {
    return name;
  }

  /**
   * 従業員の名前を設定します。
   *
   * @param name 従業員の名前
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * 早番の希望を取得します。
   *
   * @return 早番の希望
   */
  public String getEarlyWish() {
    return earlyWish;
  }

  /**
   * 早番の希望を設定します。
   *
   * @param earlyWish 早番の希望
   */
  public void setEarlyWish(String earlyWish) {
    this.earlyWish = earlyWish;
  }

  /**
   * 遅番の希望を取得します。
   *
   * @return 遅番の希望
   */
  public String getLateWish() {
    return lateWish;
  }

  /**
   * 遅番の希望を設定します。
   *
   * @param lateWish 遅番の希望
   */
  public void setLateWish(String lateWish) {
    this.lateWish = lateWish;
  }
}
