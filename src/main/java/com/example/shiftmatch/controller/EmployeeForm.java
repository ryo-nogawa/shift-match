package com.example.shiftmatch.controller;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * フォームで送信される従業員の入力データを保持します。
 */
@Getter
@Setter
@ValidTimeRange
public class EmployeeForm {

  /** 従業員の名前。 */
  @Size(max = 255, message = "氏名は255文字以内で入力してください。")
  private String name;

  /** 雇用区分（FULL_TIME, PART_TIME, MANAGER）。送信時は必須。 */
  private String employmentType;

  /** 休みのチェック。true の場合は休み。 */
  private boolean off;

  /** 勤務開始時刻（HH:mm 形式）。 */
  private String start;

  /** 勤務終了時刻（HH:mm 形式）。 */
  private String end;
}
