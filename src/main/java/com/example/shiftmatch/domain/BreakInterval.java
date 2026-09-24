package com.example.shiftmatch.domain;

import java.time.LocalTime;

/**
 * 休憩時間の区間を表すレコード。
 *
 * <p>開始時刻と終了時刻を保持します。
 */
public record BreakInterval(LocalTime startTime, LocalTime endTime) {}
