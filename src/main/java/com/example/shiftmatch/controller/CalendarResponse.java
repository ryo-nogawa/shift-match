package com.example.shiftmatch.controller;

import java.util.List;
import java.util.Map;

/**
 * カレンダー情報のレスポンスを表すレコード。
 *
 * @param month 対象月（YYYY-MM 形式）
 * @param businessDayCount 営業日数
 * @param holidayCount 祝日数
 * @param businessDays 営業日のリスト（YYYY-MM-DD 形式）
 * @param holidays 祝日のリスト（日付と祝日名を含む Map）
 */
public record CalendarResponse(
    String month,
    int businessDayCount,
    int holidayCount,
    List<String> businessDays,
    List<Map<String, String>> holidays) {}
