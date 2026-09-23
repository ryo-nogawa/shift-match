package com.example.shiftmatch.controller;

import java.util.List;

/**
 * シフト作成フォームのデータを保持します。
 */
public class ShiftForm {

  /** 従業員の入力データ一覧。 */
  private List<EmployeeForm> employees;

  /** デフォルトコンストラクタ。 */
  public ShiftForm() {}

  /**
   * 従業員の入力データ一覧を取得します。
   *
   * @return 従業員の入力データ一覧
   */
  public List<EmployeeForm> getEmployees() {
    return employees;
  }

  /**
   * 従業員の入力データ一覧を設定します。
   *
   * @param employees 従業員の入力データ一覧
   */
  public void setEmployees(List<EmployeeForm> employees) {
    this.employees = employees;
  }
}
