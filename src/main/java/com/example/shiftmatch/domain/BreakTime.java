package com.example.shiftmatch.domain;

import java.time.LocalTime;

/**
 * 従業員の休憩時間を表すレコード。
 *
 * <p>従業員、開始時刻、終了時刻を保持します。
 */
public record BreakTime(Employee employee, LocalTime start, LocalTime end) {}
