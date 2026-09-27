package com.example.shiftmatch.service;

import com.example.shiftmatch.domain.DailyWish;
import com.example.shiftmatch.domain.EmployeeProfile;
import com.example.shiftmatch.domain.ShiftAdjustment;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 営業日と従業員から、個別変更・曜日休み・既定の時間帯の優先順位に基づいて希望を決定します。
 *
 * <p>優先順位：その日の個別変更 > パートの曜日休み（休み） > 既定の時間帯（7:30〜18:30）
 *
 * <p>状態を持たない値オブジェクト相当です。
 */
public class WishResolver {

  private static final LocalTime DEFAULT_START = LocalTime.of(7, 30);
  private static final LocalTime DEFAULT_END = LocalTime.of(18, 30);

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

    // 個別変更がなければ、パートの曜日休みなら休み、それ以外は既定の時間帯
    if (profile.offDays().contains(date.getDayOfWeek())) {
      return new DailyWish(true, null, null);
    }
    return new DailyWish(false, DEFAULT_START, DEFAULT_END);
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
