package com.example.shiftmatch.controller;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * シフト作成フォームのデータを保持します。
 */
@Getter
@Setter
public class ShiftForm {

  /** 従業員の入力データ一覧。 */
  private List<EmployeeForm> employees = new ArrayList<>();
}
