package com.example.shiftmatch.controller;

import lombok.Getter;
import lombok.Setter;

/**
 * フォームで送信される個別変更のデータを保持します。
 */
@Getter
@Setter
public class AdjustmentForm {

  /** 個別変更の日付（YYYY-MM-DD 形式）。 */
  private String date;

  /** 個別変更を申請した従業員名。 */
  private String employeeName;

  /** 休みのチェック。true の場合は休み。 */
  private boolean off;

  /** 勤務開始時刻（HH:mm 形式）。 */
  private String start;

  /** 勤務終了時刻（HH:mm 形式）。 */
  private String end;
}
