package com.example.shiftmatch.controller;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 月間シフト作成フォームのデータを保持します。
 */
@Getter
@Setter
public class ShiftForm {

  /** 対象月（YYYY-MM 形式）。 */
  private String targetMonth;

  /** 従業員の入力データ一覧。 */
  private List<EmployeeForm> employees = new ArrayList<>();

  /** 日ごとの個別変更の一覧。 */
  private List<AdjustmentForm> adjustments = new ArrayList<>();
}
