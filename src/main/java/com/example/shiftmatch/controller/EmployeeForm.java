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

  /** 6 つの枠ごとの希望（Wish enum の定数名）。 */
  private List<String> wishes = new ArrayList<>();
}
