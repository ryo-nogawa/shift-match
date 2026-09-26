package com.example.shiftmatch.domain;

import java.time.LocalDate;

/**
 * 日ごとの個別変更を表すレコード。
 *
 * <p>従業員ごと、日付ごとに基本シフトから変更した希望を保持します。
 */
public record ShiftAdjustment(LocalDate date, String employeeName, DailyWish wish) {}
