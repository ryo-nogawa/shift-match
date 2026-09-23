package com.example.shiftmatch.controller;

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

  /** 早番の希望（Wish enum の定数名）。 */
  private String earlyWish;

  /** 遅番の希望（Wish enum の定数名）。 */
  private String lateWish;
}
