package com.example.shiftmatch.service;

import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.ShiftAdjustment;
import java.time.LocalDate;
import java.util.List;

/**
 * 営業日と従業員から、基本シフトと個別変更の優先順位に基づいて希望を決定します。
 *
 * <p>優先順位：その日の個別変更 > その曜日の基本シフト
 */
public class WishResolver {

  /**
   * 従業員プロファイルと日付から、優先順位に基づいた希望を決定します。
   *
   * @param profile 従業員プロファイル
   * @param date 対象日付
   * @param adjustments 個別変更のリスト
   * @return 決定した希望
   */
  public DailyWish resolve(
      EmployeeProfile profile, LocalDate date, List<ShiftAdjustment> adjustments) {
    // まず該当する個別変更を探す
    DailyWish adjustment = findMatchingAdjustment(profile.name(), date, adjustments);
    if (adjustment != null) {
      return adjustment;
    }

    // 個別変更がなければ基本シフトから該当の曜日を取得
    return profile.baseShifts().get(date.getDayOfWeek());
  }

  private DailyWish findMatchingAdjustment(
      String employeeName, LocalDate date, List<ShiftAdjustment> adjustments) {
    DailyWish lastMatch = null;
    for (ShiftAdjustment adjustment : adjustments) {
      if (adjustment.employeeName().equals(employeeName) && adjustment.date().equals(date)) {
        lastMatch = adjustment.wish();
      }
    }
    return lastMatch;
  }
}
