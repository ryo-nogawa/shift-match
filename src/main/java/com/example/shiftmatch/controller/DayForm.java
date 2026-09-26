package com.example.shiftmatch.controller;

import lombok.Getter;
import lombok.Setter;

/**
 * フォームで送信される 1 日分（曜日）の希望データを保持します。
 */
@Getter
@Setter
public class DayForm {

  /** 休みのチェック。true の場合は休み。 */
  private boolean off;

  /** 勤務開始時刻（HH:mm 形式）。 */
  private String start;

  /** 勤務終了時刻（HH:mm 形式）。 */
  private String end;
}
