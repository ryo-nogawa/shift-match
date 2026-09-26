package com.example.shiftmatch.controller;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * フォームで送信される従業員の入力データを保持します。
 */
@Getter
@Setter
public class EmployeeForm {

  /** 従業員の名前。 */
  private String name;

  /** 雇用区分（FULL_TIME, PART_TIME, MANAGER）。 */
  private String employmentType;

  /** 曜日ごとの基本シフト（月〜金）。 */
  private List<DayForm> days = new ArrayList<>();
}
