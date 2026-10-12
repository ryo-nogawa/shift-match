package com.example.shiftmatch.controller;

import com.example.shiftmatch.domain.EmploymentType;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * 従業員の個別ページ（7.3 節）の表示モデルです。
 *
 * @param month 対象月
 * @param name 従業員名
 * @param employmentType 区分
 * @param weeks 週のリスト（対象月の 1 日を含む週から末日を含む週まで）
 */
public record EmployeePageView(
    YearMonth month, String name, EmploymentType employmentType, List<Week> weeks) {

  /**
   * カレンダーの 1 週（月曜〜日曜）です。
   *
   * @param days 月曜〜日曜の 7 マス
   */
  public record Week(List<DayCell> days) {}

  /**
   * カレンダーの 1 マスです。
   *
   * @param date 日付（対象月以外の空きのマスは {@code null}）
   */
  public record DayCell(LocalDate date) {}
}
